//
//  LOBLEObserver.h
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

#define _LO_KEY_UUID @"peripheralUUID"
#define _LO_KEY_MAJOR_ID @"major"
#define _LO_KEY_MINOR_ID @"minor"
#define _LO_KEY_RSSI @"RSSI"

static NSString* const kKeyUuid = _LO_KEY_UUID;
static NSString* const kKeyMajorId = _LO_KEY_MAJOR_ID;
static NSString* const kKeyMinorId = _LO_KEY_MINOR_ID;
static NSString* const kKeyRssi = _LO_KEY_RSSI;

/**
 Optional なパラメタを設定するためのコンフィギュレーションオブジェクト
 */
@interface LOBLEEntryBuilderConfiguration : NSObject 
@property (nonatomic, strong) NSDecimalNumber* majorId;
@property (nonatomic, strong) NSDecimalNumber* minorId;
@property (nonatomic, strong) NSNumber* rssiLevel;
@end

@interface LOBLEEntryBuilder : LOEntryBuilder
-(instancetype)initWithEntryId:(NSString *)entryId enter:(BOOL)enter exit:(BOOL)exit uuid:(NSString *)uuid;
-(NSDictionary*) createWithConfigurationBlock:(void(^)(LOBLEEntryBuilderConfiguration* config))configBlock;
@end

/**
 BLE(iBeacon) の監視クラス
 */
@interface LOBLEObserver : LOLocationObserver
@end
