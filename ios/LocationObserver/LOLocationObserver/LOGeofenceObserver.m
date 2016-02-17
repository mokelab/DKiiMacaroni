//
//  LOGeofenceObserver.m
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

#import "LOGeofenceObserver.h"
#import "LOLocationObserver+Friend.h"
#import <CoreLocation/CoreLocation.h>
#import <UIKit/UIKit.h>

@interface LOGeofenceObserver()<CLLocationManagerDelegate>

@property (nonatomic, strong) CLLocationManager* locationManager;

@end

@implementation LOGeofenceObserver

-(instancetype)initWithObserver:(id)observer selector:(SEL)selector{
    self = [super initWithObserver:observer selector:selector];
    if (self) {
        _locationManager = [CLLocationManager new];
        _locationManager.delegate = self;
        _locationManager.desiredAccuracy = kCLLocationAccuracyBest;
        _locationManager.distanceFilter = kCLDistanceFilterNone;
        
        self.entries = [NSMutableDictionary new];
    }
    return self;
}

-(void)dealloc{
    if (_locationManager) {
        _locationManager.delegate = nil;
    }
}

-(void)willAddEntries{
    [self stopObserve];
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
    return [entry objectForKey:kKeyLatitude] != 0
    && [entry objectForKey:kKeyLongitude] != 0
    && [entry objectForKey:kKeyRange] != 0;
}

//private

-(void)startObserve{
    if (self.entries.count > 0) {
        float iOSVersion = [[[UIDevice currentDevice] systemVersion] floatValue];
        
        if ([_locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
            [_locationManager requestAlwaysAuthorization];
        }
        
        for (NSDictionary* entry in self.entries.allValues) {
            double latitude = ((NSNumber*)[entry objectForKey:kKeyLatitude]).doubleValue;
            double longitude = ((NSNumber*)[entry objectForKey:kKeyLongitude]).doubleValue;
            long range = ((NSNumber*)[entry objectForKey:kKeyRange]).longValue;
            
            //最大半径に丸める
            if (range > _locationManager.maximumRegionMonitoringDistance) {
                range = _locationManager.maximumRegionMonitoringDistance;
            }
            
            CLLocationCoordinate2D center = CLLocationCoordinate2DMake(latitude, longitude);
            
            if (iOSVersion >= 7.0) {
                CLCircularRegion* region = [[CLCircularRegion alloc] initWithCenter:center radius:range identifier:entry[kKeyEntryId]];
                [_locationManager startMonitoringForRegion:region];
            }else{
                CLRegion* region = [[CLRegion alloc] initCircularRegionWithCenter:center radius:range identifier:entry[kKeyEntryId]];
                
                if (iOSVersion >= 5.0) {
                    [_locationManager startMonitoringForRegion:region];
                }else{
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
                    [_locationManager startMonitoringForRegion:region desiredAccuracy:kCLLocationAccuracyBest];
#pragma clang diagnostic pop
                }
            }
        }
    }
}

-(void)stopObserve{
    for (CLRegion* region in _locationManager.monitoredRegions) {
        if ([region isKindOfClass:[CLCircularRegion class]]) {
            [_locationManager stopMonitoringForRegion:region];
        }
    }
}

-(void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region{
    
}

-(void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(CLRegion *)region withError:(NSError *)error{
    
}

//LocationManagerDelegate
-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region{
    if ([region isKindOfClass:[CLCircularRegion class]]) {
        NSDictionary* entry = [self.entries objectForKey:region.identifier];
        if ([entry[kKeyTransitionEnter] boolValue]) {
            LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry action:REGION_ENTER];
            [self postResult:result];
        }
    }
}

-(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region{
    if ([region isKindOfClass:[CLCircularRegion class]]) {
        NSDictionary* entry = [self.entries objectForKey:region.identifier];
        if ([entry[kKeyTransitionExit] boolValue]) {
            LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry action:REGION_EXIT];
            [self postResult:result];
        }
    }
}

@end
