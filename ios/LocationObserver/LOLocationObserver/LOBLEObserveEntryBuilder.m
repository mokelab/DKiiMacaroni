//
//  LOBLEEntryBuilder.m
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

#import "LOBLEObserver.h"
#import "LOEntryBuilder+Friend.h"
#import "Common.h"

@implementation LOBLEEntryBuilderConfiguration
-(instancetype)init{
    self = [super init];
    if (self) {
        _rssiLevel = 0;
    }
    return self;
}
@end

@interface LOBLEEntryBuilder()
@property (nonatomic, strong, readonly) NSString* uuid;
@end

@implementation LOBLEEntryBuilder

-(instancetype)init{
    FobiddneCallMethod();
    return nil;
}

-(instancetype)initWithEntryId:(NSString *)entryId enter:(BOOL)enter exit:(BOOL)exit uuid:(NSString *)uuid{
    self = [super initWithEntryId:entryId enter:enter exit:exit];
    if (self) {
        _uuid = uuid;
    }
    return self;
}

-(NSDictionary *)createWithConfigurationBlock:(void (^)(LOBLEEntryBuilderConfiguration *))configBlock{
    NSMutableDictionary* dict = [self create];
    
    if (configBlock) {
        LOBLEEntryBuilderConfiguration* config = [LOBLEEntryBuilderConfiguration new];
        configBlock(config);
        if (config.majorId) {
            [dict setObject:config.majorId forKey:kKeyMajorId];
        }
        if (config.minorId) {
            [dict setObject:config.minorId forKey:kKeyMinorId];
        }
        if (config.rssiLevel) {
            [dict setObject:config.rssiLevel forKey:kKeyRssi];
        }
    }
    
    [dict setObject:_uuid forKey:kKeyUuid];
    
    return dict;
}

@end
