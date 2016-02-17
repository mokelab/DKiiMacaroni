//
//  ContentTriggerEntry.h
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

@class KiiObject;

extern NSString* const kTriggerTypeBLE;
extern NSString* const kTriggerTypeGeofence;
extern NSString* const kTriggerTypeQR;
extern NSString* const kTriggerTypeTime;

/**
 トリガの監視時間に関する種別
 */
typedef NS_ENUM(NSInteger, NotificationType){
    CTLNotificationTypeStart,
    CTLNotificationTypeEnd
};

// entry の各情報を参照するためのキー
//Geofence
extern NSString* const kCTLContentTriggerKeyLatitude;
extern NSString* const kCTLContentTriggerKeyLongitude;
extern NSString* const kCTLContentTriggerKeyRadius;

//iBeacon
extern NSString* const kCTLContentTriggerKeyProximityUUID;
extern NSString* const kCTLContentTriggerKeyMajorId;
extern NSString* const kCTLContentTriggerKeyMinorId;
extern NSString* const kCTLContentTriggerKeyRSSI;

//QR
extern NSString* const kCTLContentTriggerKeyQrTarget;

@interface CTLTermParams : NSObject
@property (nonatomic, strong) NSString* startTime;
@property (nonatomic, strong) NSString* endTime;
@property (nonatomic, strong) NSNumber* startDateTime;
@property (nonatomic, strong) NSNumber* endDateTime;
@property (nonatomic, assign) NSInteger dayOfWeekFlags;
@property (nonatomic, strong) NSNumber* dayOfMonth;

-(instancetype)initWithKiiObject:(KiiObject*)kiiObject;

-(NSDate*)nextStartDateTime;
-(NSDate*)nextEndDateTime;
-(NSDate*)prevStartDateTime;
-(NSDate*)prevEndDateTime;
@end

/**
 すべての情報を含んだ トリガ情報
 */
@interface CTLEntry : NSObject

/**
 トリガID
 */
@property (nonatomic, strong, readonly) NSString* entryId;

/**
 トリガのタイトル
 */
@property (nonatomic, strong, readonly) NSString* title;

/**
 トリガの説明
 */
@property (nonatomic, strong, readonly) NSString* entryDescription;

/**
 コンテンツを含む KiiObject のID
 */
@property (nonatomic, strong, readonly) NSString* masterId;

/**
 トリガ情報
 */
@property (nonatomic, strong, readonly) NSDictionary* entry;

/**
 トリガを監視するタイミング
 */
@property (nonatomic, strong, readonly) CTLTermParams* termParams;

@property (nonatomic, strong, readonly) NSString* triggerType;

-(instancetype)initWithKiiObject:(KiiObject*)kiiObject;
@end
