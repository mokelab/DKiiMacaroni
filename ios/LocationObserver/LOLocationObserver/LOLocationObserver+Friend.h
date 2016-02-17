//
//  LOLocationObserver+Friend.h
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

#ifndef LOLocationObserver_LOLocationObserver_Friend_h
#define LOLocationObserver_LOLocationObserver_Friend_h

/**
 Abstract 監視クラス (Friend)
 */
@interface LOLocationObserver(Friend)

/**
 トリガ情報マップ
 */
@property (nonatomic, strong) NSMutableDictionary* entries;

/**
 トリガを検知した際に呼び出し、登録されたコールバックを呼び出す。
 */
-(void)postResult:(LOObserveResult*)result;

/**
 トリガを追加する直前に呼び出される。必要に応じて監視の一時停止などを行う。
 */
-(void)willAddEntries;

/**
 トリガの追加が完了した直後に呼び出される。必要に応じて監視の再開などを行う。
 */
-(void)didAddEntries;

-(void)startObserve;
-(void)stopObserve;

/**
 トリガを削除する直前に呼び出される。必要に応じて監視の一時停止などを行う。
 */
-(void)willRemoveEntries;
-(void)didRemoveEntries:(BOOL)entryIsNothing;
-(BOOL)isMineEntry:(NSDictionary*)entry;
@end

#endif
