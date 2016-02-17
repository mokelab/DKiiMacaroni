//
//  ContentTriggerObserver.m
//  ContentTriggerLibrary
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

#import "CTLContentTrigger.h"
#import "CTLObserver.h"
#import "CTLEntry.h"
#import "CTLObserveResult.h"
#import <CTLLocationObserver.h>
#import <LOLocationObserver/LOBLEObserver.h>
#import <LOLocationObserver/LOGeofenceObserver.h>
#import "CTLTimeObserver.h"
#import "CTLQRObserver.h"

@interface CTLObserver()
@property (nonatomic, strong) NSMutableDictionary* entries;  //<NSString:entryId, ContentTriggerEntry:entry>
@property (nonatomic, strong) NSMutableDictionary* observers;   //<NSString:triggerType, CTLLocationObserver:observer>
@property (nonatomic, strong) CTLQRObserver* qrObserverCache;
@property (nonatomic, strong) CTLTimeObserver* timeObserverCache;
@end

@implementation CTLObserver

-(instancetype)init{
    self = [super init];
    if (self) {
        _entries = [NSMutableDictionary new];
        
        _timeObserverCache = [[CTLTimeObserver alloc] initWithObserver:self selector: @selector(observeTrigger:)];
        _qrObserverCache = [[CTLQRObserver alloc] initWithObserver:self selector:@selector(observeTrigger:)];
        
        _observers = [NSMutableDictionary new];
        [_observers setObject:[CTLLocationObserverWrapper locationObserver:[[LOBLEObserver alloc] initWithObserver:self selector:@selector(observeTrigger:)]] forKey:kTriggerTypeBLE];
        [_observers setObject:[CTLLocationObserverWrapper locationObserver:[[LOGeofenceObserver alloc] initWithObserver:self selector:@selector(observeTrigger:)]] forKey:kTriggerTypeGeofence];
        [_observers setObject:_timeObserverCache forKey:kTriggerTypeTime];
        [_observers setObject:_qrObserverCache forKey:kTriggerTypeQR];
        
        [_timeObserverCache verificationAndUpdateLocalNotification];
    }
    return self;
}

-(void)addTriggerEntry:(CTLEntry*)entry{
    if (entry.entryId != nil) {
        NSObject<CTLLocationObserver>* observer = _observers[entry.triggerType];
        if (observer) {
            [_entries setObject:entry forKey:entry.entryId];
            [observer addEntry:entry];
        }
    }
}

-(void)removeTriggerEntryWithEntryId:(NSString*)entryId{
    if (entryId != nil) {
        [_entries removeObjectForKey:entryId];
        for (NSObject<CTLLocationObserver>* observer in _observers.allValues) {
            [observer removeEntryWithEntryId:entryId];
        }
    }
}

-(void)removeAllEntries{
    for (NSObject<CTLLocationObserver>* observer in _observers.allValues) {
        [observer removeAllEntry];
    }
}

-(void)setEnabledLOTriggerType:(NSString*)triggerType enabled:(BOOL)enabled{
    NSObject<CTLLocationObserver>* observer = _observers[triggerType];
    if (observer) {
        [observer setEnabled:enabled];
    }
}

-(void)queryQRString:(NSString *)qrStr{
    [_qrObserverCache queryQRString:qrStr];
}

-(void)verificationAndUpdateLocalNotification{
    [_timeObserverCache verificationAndUpdateLocalNotification];
}

#pragma mark - for selector.

-(void)observeTrigger:(LOObserveResult*) result{
    CTLEntry* entry = _entries[result.observeEntry[kKeyEntryId]];
    CTLObserveResult* ctResult = [[CTLObserveResult alloc] initWithEntry:entry action:result.action];
    [_delegate contentTriggerObserver:self observeTrigger:ctResult];
}

@end
