//
//  ContentTriggerObserveResult.m
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

#import "CTLObserveResult.h"
@interface CTLObserveResult()<NSCoding>
@end

@implementation CTLObserveResult

-(instancetype) initWithEntry:(CTLEntry*)entry action:(NSInteger)action{
    self = [super init];
    if (self) {
        _observeEntry = entry;
        _action = action;
    }
    return self;
}

#pragma mark - NSCoding

-(void)encodeWithCoder:(NSCoder *)aCoder{
    [aCoder encodeObject:[NSKeyedArchiver archivedDataWithRootObject:_observeEntry]];
    [aCoder encodeInteger:_action forKey:@"action"];
}

-(id)initWithCoder:(NSCoder *)aDecoder{
    self = [super init];
    if (self) {
        _observeEntry = [NSKeyedUnarchiver unarchiveObjectWithData:[aDecoder decodeObject]];
        _action = [aDecoder decodeIntegerForKey:@"action"];
    }
    return self;
}

@end
