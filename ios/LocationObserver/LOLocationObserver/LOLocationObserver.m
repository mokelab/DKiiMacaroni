//
//  LOLocationObserver.m
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

#import "LOLocationObserver.h"
#import "LOLocationObserver+Friend.h"
#import "Common.h"

NSString* const kKeyEntryId = @"entryId";
NSString* const kKeyTransitionEnter = @"transitionEnter";
NSString* const kKeyTransitionExit = @"transitionExit";

@interface LOLocationObserver()
@property (nonatomic, weak) id observer;
@property (nonatomic, assign) SEL selector;

@property (nonatomic, strong) NSMutableDictionary* entries;
@end

@implementation LOLocationObserver
-(instancetype)init{
    FobiddneCallMethod();
    return nil;
}

-(instancetype)initWithObserver:(id)observer selector:(SEL)selector{
    self = [super init];
    if (self) {
        _observer = observer;
        _selector = selector;
        _enabled = YES;
        
        _entries = @{}.mutableCopy;
    }
    return self;
}

-(void)setEnabled:(BOOL)enabled{
    if (_enabled ^ enabled) {
        _enabled = enabled;
        if (_enabled) {
            [self startObserve];
        }else{
            [self stopObserve];
        }
    }
}

-(void)startObserve{
    UndefineMethod();
}

-(void)stopObserve{
    UndefineMethod();
}

-(void)addEntry:(NSDictionary *)entry{
    if ([self isMineEntry:entry]) {
        [self willAddEntries];
        [_entries setObject:entry forKey:entry[kKeyEntryId]];
        [self didAddEntries];
    }
}

-(void)addEntries:(NSArray *)entries{
    if (entries.count > 0) {
        [self willAddEntries];
        for (NSDictionary* entry in entries) {
            if ([self isMineEntry:entry]) {
                [_entries setObject:entry forKey:entry[kKeyEntryId]];
            }
        }
        [self didAddEntries];
    }
}

-(void)removeEntryWithEntryId:(NSString *)entryId{
    if ([[self entries].allKeys containsObject:entryId]) {
        [self willRemoveEntries];
        [_entries removeObjectForKey:entryId];
        [self didRemoveEntries:[self entries].count == 0];
    }
}

-(void)removeAllEntry{
    if (_entries.count > 0) {
        [self willRemoveEntries];
        [_entries removeAllObjects];
        [self didRemoveEntries:_entries.count == 0];
    }
}

-(void)willAddEntries{
    
}
-(void)didAddEntries{
    
}
-(void)willRemoveEntries{
    
}
-(void)didRemoveEntries:(BOOL)entryIsNothing{
    
}
-(BOOL)isMineEntry:(NSDictionary*)entry{
    return YES;
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
