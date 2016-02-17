//
//  CTLTimeObserver.m
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

#import <UIKit/UIKit.h>
#import <CTLContentTrigger.h>
#import "CTLContentTrigger+Friend.h"
#import <CTLTimeObserver.h>
#import <LOLocationObserver/LOLocationObserver+Friend.h>

@interface CTLTimeObserver()
@property (strong, nonatomic) NSMutableDictionary* entries; //NSMutableDictionary<NSString*, CTLEntry*>
@property (strong, nonatomic) NSMutableArray* validNotifications;   //NSMutableArray<UILocalNotification*>
@property (strong, nonatomic) NSMutableArray* detectedEntryIds;
@property (assign, nonatomic) BOOL enabled;

@property (nonatomic, strong) id observer;
@property (nonatomic, assign) SEL selector;
@end

@implementation CTLTimeObserver

-(instancetype)initWithObserver:(id)observer selector:(SEL)selector{
    self = [super init];
    if (self) {
        _enabled = YES;
        _entries = [NSMutableDictionary dictionary];
        _validNotifications = [NSMutableArray array];
        
        _observer = observer;
        _selector = selector;
        [self loadUserDefaults];
    }
    return self;
}

#pragma mark - Inner function.

-(void)loadUserDefaults{
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    _detectedEntryIds = [userDefaults arrayForKey:@"detectedEntryIds"].mutableCopy;
    if (_detectedEntryIds == nil) {
        _detectedEntryIds = [NSMutableArray array];
    }
}

-(void)saveUserDefaults{
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults setObject:_detectedEntryIds forKey:@"detectedEntryIds"];
    [userDefaults synchronize];
}

-(void)startObserve{
    //noop
}

-(void)stopObserve{
    //noop
}

-(void)addEntry:(CTLEntry *)entry{
    [_entries setObject:entry forKey:entry.entryId];
}

-(void)removeAllEntry{
    [_entries removeAllObjects];
}

-(void)removeEntryWithEntryId:(NSString *)entryId{
    [_entries removeObjectForKey:entryId];
}

-(void)setEnabled:(BOOL)enabled{
    _enabled = enabled;
}

-(void)postResult:(LOObserveResult *)result{
    if ([_observer respondsToSelector:_selector]) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [_observer performSelector:_selector withObject:result];
#pragma clang diagnostic pop
    }
}

#pragma mark - Time trigger management.

-(void)verificationAndUpdateLocalNotification{
    UIApplication* application = [UIApplication sharedApplication];
    for (UILocalNotification* localNotification in _validNotifications) {
        [application cancelLocalNotification:localNotification];
    }
    [_validNotifications removeAllObjects];
    
    for (NSString* key in _entries.allKeys) {
        CTLEntry* entry = _entries[key];
        if ([entry.triggerType isEqual:kTriggerTypeTime]) {
            NSDate* nextStartDateTime = entry.termParams.nextStartDateTime;
            
            if (nextStartDateTime) {
                UILocalNotification* localNotification = [CTLContentTrigger notificationWithResult:[[CTLObserveResult alloc] initWithEntry:entry action:REGION_NONE]];
                localNotification.fireDate = nextStartDateTime;
                
                [_validNotifications addObject:localNotification];
                [application scheduleLocalNotification:localNotification];
            }
        }
    }
}

@end
