//
//  ContentTriggerObserveResult.h
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
#import "CTLEntry.h"

@interface CTLObserveResult : NSObject


/**
 検出のきっかけとなったトリガ情報
 */
@property(nonatomic, strong, readonly) CTLEntry* observeEntry;

/**
 検出のきっかけとなった Action
 @see REGION_ENTER
 @see REGION_EXIT
 */
@property(nonatomic, assign, readonly) NSInteger action;

-(instancetype) initWithEntry:(CTLEntry*)entry action:(NSInteger)action;
@end
