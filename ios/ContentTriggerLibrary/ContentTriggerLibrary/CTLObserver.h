//
//  ContentTriggerObserver.h
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
#import <LOLocationObserver/LOLocationObserver.h>
#import "CTLObserveResult.h"

@class CTLObserver;

@protocol CTLObserverCallback <NSObject>
-(void)contentTriggerObserver:(CTLObserver*)contentTriggerObserver observeTrigger:(CTLObserveResult*) result;
@end

/**
 Observer と ContentTrigger の Adapter
 */
@interface CTLObserver : NSObject

@property (nonatomic, weak) NSObject<CTLObserverCallback>* delegate;

-(void)addTriggerEntry:(CTLEntry*)entry;
-(void)removeTriggerEntryWithEntryId:(NSString*)entryId;
-(void)removeAllEntries;

-(void)setEnabledLOTriggerType:(NSString*)triggerType enabled:(BOOL)enabled;

/**
 現在有効なQRコードのトリガから指定文字列にマッチするものを検索する。
 */
-(void)queryQRString:(NSString *)qrStr;

/**
 未来に有効な時刻トリガのみが有効になるように再構成する。
 */
-(void)verificationAndUpdateLocalNotification;
@end
