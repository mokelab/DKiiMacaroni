//
//  LOEntryBuilder.m
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
#import "Common.h"

@interface LOEntryBuilder()
@property (nonatomic, strong) NSString* entryId;
@property (nonatomic, assign) BOOL enter;
@property (nonatomic, assign) BOOL exit;
@end

@implementation LOEntryBuilder

-(instancetype)init{
    FobiddneCallMethod();
    return nil;
}

-(instancetype)initWithEntryId:(NSString*)entryId enter:(BOOL)enter exit:(BOOL)exit{
    self = [super init];
    if (self) {
        _entryId = entryId;
        _enter = enter;
        _exit = exit;
    }
    return self;
}

-(NSMutableDictionary*)create{
    return @{kKeyEntryId:_entryId, kKeyTransitionEnter:@(_enter), kKeyTransitionExit:@(_exit)}.mutableCopy;
}

@end
