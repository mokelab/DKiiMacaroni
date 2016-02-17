//
//  CTLLocationObserverWrapper.m
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

#import "CTLLocationObserver.h"
#import <CTLEntry.h>

@interface CTLLocationObserverWrapper()
@end

@implementation CTLLocationObserverWrapper

+(instancetype)locationObserver:(LOLocationObserver *)locationObserver{
    CTLLocationObserverWrapper* _self = [[CTLLocationObserverWrapper alloc] initWithLocationObserver:locationObserver];
    return _self;
}

-(instancetype)initWithLocationObserver:(LOLocationObserver *)locationObserver{
    self = [super init];
    if (self) {
        _locationObserver = locationObserver;
    }
    return self;
}

-(void)addEntry:(CTLEntry*)entry{
    [_locationObserver addEntry:entry.entry];
}

-(void)removeAllEntry{
    [_locationObserver removeAllEntry];
}

-(void)removeEntryWithEntryId:(NSString *)entryId{
    [_locationObserver removeEntryWithEntryId:entryId];
}

-(void)setEnabled:(BOOL)enabled{
    [_locationObserver setEnabled:enabled];
}

@end
