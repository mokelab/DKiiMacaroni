//
//  CTLQRObserver.m
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

#import "CTLQRObserver.h"
#import <LOLocationObserver/LOLocationObserver+Friend.h>

NSString* const kCTLContentTriggerKeyQrTarget = @"qr_target";

@interface CTLQRObserver()
@property (nonatomic, assign) BOOL enabled;
@property (nonatomic, strong) NSMutableDictionary* entries;

@property (nonatomic, strong) id observer;
@property (nonatomic, assign) SEL selector;
@end

@implementation CTLQRObserver

-(instancetype)initWithObserver:(id)observer selector:(SEL)selector{
    self = [super init];
    if (self) {
        _enabled = YES;
        _entries = [NSMutableDictionary dictionary];
        
        _observer = observer;
        _selector = selector;
    }
    return self;
}

-(void)queryQRString:(NSString *)str{
    if (self.enabled) {
        for (CTLEntry* entry in [self entries].allValues) {
            NSString* entryStr = entry.entry[kCTLContentTriggerKeyQrTarget];
            if (entryStr == nil) {
                continue;
            }
            
            if ([entryStr compare:str] == NSOrderedSame) {
                LOObserveResult* result = [[LOObserveResult alloc] initWithEntry:entry.entry action:0];
                [self postResult:result];
            }
        }
    }
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
@end
