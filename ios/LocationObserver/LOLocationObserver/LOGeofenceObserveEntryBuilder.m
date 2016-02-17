//
//  LOGeofenceEntryBuilder.m
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

#import "LOEntryBuilder+Friend.h"
#import "LOLocationObserver+Friend.h"
#import "LOGeofenceObserver.h"
#import "Common.h"

@interface LOGeofenceEntryBuilder()
@property(nonatomic, assign) double latitude;
@property(nonatomic, assign) double longitude;
@property(nonatomic, assign) double range;
@end

@implementation LOGeofenceEntryBuilder

-(instancetype)init{
    FobiddneCallMethod();
    return nil;
}

-(instancetype)initWithEntryId:(NSString *)entryId enter:(BOOL)enter exit:(BOOL)exit latitude:(double)latitude longitude:(double)longitude range:(double)range{
    self = [super initWithEntryId:entryId enter:enter exit:exit];
    if (self) {
        _latitude = latitude;
        _longitude = longitude;
        _range = range;
    }
    return self;
}

-(NSDictionary *)create{
    NSMutableDictionary* entry = [super create];
    [entry addEntriesFromDictionary:@{kKeyLatitude:@([self latitude]), kKeyLongitude:@([self longitude]), kKeyRange:@([self range])}];
    return entry;
}

@end
