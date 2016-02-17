//
//  ContentTrigger.h
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

#ifndef ContentTriggerLibrary_ContentTrigger_h
#define ContentTriggerLibrary_ContentTrigger_h

#import <LOLocationObserver/LOLocationObserver.h>
#import <UIKit/UIKit.h>
#import "CTLObserveResult.h"
#import "CTLEntry.h"

@class KiiObject;

/**
 トリガが検知された際に発行される Notification の Name;
 */
extern NSString* const CTLNotificationName;

/**
 トリガ情報の更新処理に対するコールバックブロック
 */
typedef void (^CTLFetchCompletionHandler)(NSArray* triggers, NSError* error);

/**
 コンテンツ情報の取得処理に対するコールバックブロック
 */
typedef void (^CTLRequestContentCompletionHandler)(CTLEntry* entry, NSDictionary* result, NSError* error);
/**
 コンテンツの画像取得に対するコールバックブロック
 */
typedef void (^CTLRequestUIImageCompletionHandler)(KiiObject* object, UIImage* image, NSError* error);

@class CTLContentTrigger;

/**
 トリガ情報を更新、監視、コールバックする機能を提供するクラス
 */
@interface CTLContentTrigger : NSObject

/**
 トリガ情報を格納している KiiGroup の ID
 */
@property (nonatomic, strong) NSString* groupId;

+(instancetype)sharedInstance;

/**
 Kii のストレージにアクセスし、トリガ情報を取得する。
 */
-(void)fetchWithBlock:(CTLFetchCompletionHandler)block;

/**
 トリガ情報を更新する。
 */
-(void)updateTriggers:(NSArray*)triggers;

/**
 トリガの種類を指定して、検出機能を設定する。
 */
-(void)setEnabledLOTriggerType:(NSString*)triggerType enabled:(BOOL)enabled;

/**
 現在有効なQRコードのトリガから指定文字列にマッチするものを検索する。
 */
-(void)queryQRString:(NSString*)qrStr;

-(void)loadEntriesAtLastTime;

/**
 トリガに対するコンテンツ情報を取得する。
 */
-(void)loadContentWithObserveResult:(CTLObserveResult*)result block:(CTLRequestContentCompletionHandler)block;

/**
 時刻トリガを再構築する。
 */
-(void)didBackgroundFetch;

#pragma mark - Methods to convert the notifciation.

/**
 userInfo が検知結果のものであればそれを取得する
 */
-(CTLObserveResult*)observerResultFromUserInfo:(NSDictionary*)userInfo;

@end

#endif
