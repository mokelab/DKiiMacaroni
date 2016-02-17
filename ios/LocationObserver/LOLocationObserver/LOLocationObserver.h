//
//  LOLocationObserver.h
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

#import <Foundation/Foundation.h>

//! Project version number for LOLocationObserver.
FOUNDATION_EXPORT double LOLocationObserverVersionNumber;

//! Project version string for LOLocationObserver.
FOUNDATION_EXPORT const unsigned char LOLocationObserverVersionString[];

// In this header, you should import all the public headers of your framework using statements like #import <LOLocationObserver/PublicHeader.h>

#define REGION_ENTER 1
#define REGION_EXIT 2
#define REGION_NONE 3

extern NSString* const kKeyEntryId;
extern NSString* const kKeyTransitionEnter;
extern NSString* const kKeyTransitionExit;

/**
 トリガ情報ビルダ
 */
@interface LOEntryBuilder : NSObject
-(NSMutableDictionary*)create;
@end

/**
 検出結果
 */
@interface LOObserveResult : NSObject

-(instancetype)initWithEntry:(id)entry action:(NSInteger)action;

/**
 検出のきっかけとなったトリガ情報
 */
@property(nonatomic, strong, readonly) NSDictionary* observeEntry;

/**
 検出のきっかけとなった Action
 @see REGION_ENTER
 @see REGION_EXIT
 */
@property(nonatomic, assign, readonly) NSInteger action;
@end

@protocol LOLocationObserverProtocol <NSObject>
-(void)addEntry:(NSDictionary*)entry;
-(void)addEntries:(NSArray*)entries;
-(void)removeEntryWithEntryId:(NSString*)entryId;
-(void)removeAllEntry;
@end

/**
 Abstract 監視クラス
 */
@interface LOLocationObserver : NSObject<LOLocationObserverProtocol>
@property (nonatomic, assign) BOOL enabled;

/**
 コールバック先のオブジェクトとセレクタを指定して初期化する
 */
-(instancetype)initWithObserver:(id)observer selector:(SEL)selector;
@end
