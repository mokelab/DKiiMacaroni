//
//  CTLLocationObserverWrapper.h
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

#import <Foundation/Foundation.h>
#import "LOLocationObserver/LOLocationObserver.h"
#import <CTLEntry.h>

@protocol CTLLocationObserver <NSObject>
-(instancetype)initWithObserver:(id)observer selector:(SEL)selector;
-(void)addEntry:(CTLEntry*)entry;
-(void)removeEntryWithEntryId:(NSString*)entryId;
-(void)removeAllEntry;

-(void)setEnabled:(BOOL)enabled;
@end

@interface CTLLocationObserverWrapper : NSObject<CTLLocationObserver>
@property (strong, readonly, nonatomic) LOLocationObserver* locationObserver;

+(instancetype)locationObserver:(LOLocationObserver*)locationObserver;
-(instancetype)initWithLocationObserver:(LOLocationObserver*)locationObserver;
@end
