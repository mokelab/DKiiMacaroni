//
//  LOBLEObserver.m
//  LOLocationObserver
//
// Copyright 2016 Kii Consortium
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#import "LOBLEObserver.h"
#import "LOLocationObserver+Friend.h"
#import <CoreLocation/CoreLocation.h>
#import <UIKit/UIKit.h>

@interface LOBLEObserver()<CLLocationManagerDelegate>
@property (nonatomic, strong) CLLocationManager* locationManager;

@property (nonatomic, strong) NSMutableDictionary* pastEnterdRegions;
@end

@implementation LOBLEObserver

-(instancetype)initWithObserver:(id)observer selector:(SEL)selector{
    self = [super initWithObserver:observer selector:selector];
    if (self) {
        _locationManager = [CLLocationManager new];
        _locationManager.delegate = self;
        
        _pastEnterdRegions = [NSMutableDictionary new];
    }
    return self;
}

-(void)didAddEntries{
    if (self.enabled) {
        [self startObserve];
    }
}

-(void)willRemoveEntries{
    [self stopObserve];
}

-(void)didRemoveEntries:(BOOL)entryIsNothing{
    if (!entryIsNothing && self.enabled) {
        [self startObserve];
    }
}

-(BOOL)isMineEntry:(NSDictionary *)entry{
    return [entry objectForKey:kKeyUuid] != nil;
}

-(void)startObserve{
    if (self.entries.count > 0) {
        if ([_locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
            [_locationManager requestAlwaysAuthorization];
        }
        
        for (NSDictionary* entry in self.entries.allValues) {
            NSString* uuidStr = [entry objectForKey:kKeyUuid];
            NSDecimalNumber* major = [entry objectForKey:kKeyMajorId];
            NSDecimalNumber* minor = [entry objectForKey:kKeyMinorId];
            NSNumber* rssi = [entry objectForKey:kKeyRssi];
            
            NSUUID* uuid = [[NSUUID alloc] initWithUUIDString:uuidStr];
            
            CLBeaconRegion* region = nil;
            if (minor) {
                region = [[CLBeaconRegion alloc] initWithProximityUUID:uuid major:major.intValue minor:minor.intValue identifier:entry[kKeyEntryId]];
            }else if (major){
                region = [[CLBeaconRegion alloc] initWithProximityUUID:uuid major:major.intValue identifier:entry[kKeyEntryId]];
            }else if (uuid){
                region = [[CLBeaconRegion alloc] initWithProximityUUID:uuid identifier:entry[kKeyEntryId]];
            }
            
            if (region) {
                region.notifyOnEntry = YES;
                region.notifyOnExit = YES;
                region.notifyEntryStateOnDisplay = NO;
                
                if (rssi) {
                    [_locationManager startRangingBeaconsInRegion:region];
                }else{
                    [_locationManager startMonitoringForRegion:region];
                }
            }
        }
    }
}

-(void)stopObserve{
    for (CLRegion* region in _locationManager.monitoredRegions) {
        if ([region isKindOfClass:[CLBeaconRegion class]]) {
            [_locationManager stopMonitoringForRegion:region];
        }
    }
    for (CLBeaconRegion* region in _locationManager.rangedRegions) {
        [_locationManager stopRangingBeaconsInRegion:region];
    }
}

//LocationManagerDelegate
-(void)locationManager:(CLLocationManager *)manager rangingBeaconsDidFailForRegion:(CLBeaconRegion *)region withError:(NSError *)error{
    if (error) {
        if (error.code == 16) {
            //BlueTooth is ofline.
        }
    }
}

-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region{
    if ([region isKindOfClass:[CLBeaconRegion class]]) {
        NSDictionary* entry = [self.entries objectForKey:region.identifier];
        LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry action:REGION_ENTER];
        [self postResult:result];
    }
}

-(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region{
    if ([region isKindOfClass:[CLBeaconRegion class]]) {
        NSDictionary* entry = [self.entries objectForKey:region.identifier];
        LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry action:REGION_EXIT];
        [self postResult:result];
    }
}

-(void)locationManager:(CLLocationManager *)manager didRangeBeacons:(NSArray *)beacons inRegion:(CLBeaconRegion *)region{
    NSDictionary* entry = [self.entries objectForKey:region.identifier];
    NSNumber* rssi = [entry objectForKey:kKeyRssi];
    
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"proximity != %d", CLProximityUnknown];
    NSArray *validBeacons = [beacons filteredArrayUsingPredicate:predicate];
    
    CLBeacon* nowBeacon = nil;
    for (CLBeacon* beacon in validBeacons) {
        if (nowBeacon == nil && (rssi == nil || rssi.longValue <= beacon.rssi)) {
            nowBeacon = beacon;
            continue;
        }
        
        nowBeacon = nowBeacon.rssi > beacon.rssi ? nowBeacon : beacon;
    }
    
    int action = 0;
    
    CLBeacon* pastBeacon = [_pastEnterdRegions objectForKey:region.identifier];
    
    if (pastBeacon && nowBeacon) {
        if (rssi) {
            long rssiLong = rssi.longValue;
            if (pastBeacon.rssi < rssiLong && rssiLong <= nowBeacon.rssi) {
                action = REGION_ENTER;
            }else if (pastBeacon.rssi >= rssiLong && rssiLong > nowBeacon.rssi){
                action = REGION_EXIT;
            }
        }
    }else if(pastBeacon){
        action = REGION_EXIT;
    }else if(nowBeacon){
        action = REGION_ENTER;
    }
    
    if (nowBeacon) {
        [_pastEnterdRegions setObject:nowBeacon forKey:region.identifier];
    }else{
        [_pastEnterdRegions removeObjectForKey:region.identifier];
    }
    
    if (action > 0) {
        if (action == REGION_ENTER && ![entry[kKeyTransitionEnter] boolValue]) {
            return;
        }
        if (action == REGION_EXIT && ![entry[kKeyTransitionExit] boolValue]) {
            return;
        }
        
        LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry action:action];
        [self postResult:result];
    }
}

@end
