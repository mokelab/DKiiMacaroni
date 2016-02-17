///
//  ContentTrigger.m
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
#import <UIKit/UIKit.h>
#import "CTLContentTrigger.h"
#import "CTLObserver.h"
#import "CTLEntry.h"
#import "CTLObjectBodyProvider.h"
#import <KiiSDK/Kii.h>

NSString* const CTLNotificationName = @"content_trigger.notification";

@interface CTLContentTrigger()<CTLObserverCallback>
@property (nonatomic, strong) NSMutableDictionary* entries;  //<NSString:entryId, ContentTriggerEntry:entry>

@property (nonatomic, strong) CTLObserver* contentTriggerObserver;
@end

@implementation CTLContentTrigger

static NSString* const kUserDefaultsKey = @"content_trigger_observer.user_defaults";
static NSString* const kBucketName = @"trigger";

-(instancetype)init{
    self = [super init];
    if (self) {
        _contentTriggerObserver = [CTLObserver new];
        _contentTriggerObserver.delegate = self;
        
        _entries = [NSMutableDictionary new];
    }
    return self;
}

+(instancetype)sharedInstance{
    static CTLContentTrigger* sharedInstance;
    static dispatch_once_t oncePredicate;
    dispatch_once(&oncePredicate, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

-(void)fetchWithBlock:(CTLFetchCompletionHandler)block{
    //KiiGroup* =
    id group = [NSClassFromString(@"KiiGroup") performSelector:@selector(groupWithID:) withObject:_groupId];
    
    NSMutableArray* resultArray = [[NSMutableArray alloc] init];
    
    //KiiQuery* =
    id clause = [NSClassFromString(@"KiiClause") performSelector:@selector(equals:value:) withObject:@"enabled" withObject:[NSNumber numberWithBool:YES]];
    id query = [NSClassFromString(@"KiiQuery") performSelector:@selector(queryWithClause:) withObject:clause];
    [self nextExecuteQuery:group query:query block:block resultReceiver:resultArray];
}

-(void)updateTriggers:(NSArray*)triggers{
    
    [_entries removeAllObjects];
    for (CTLEntry* entry in triggers) {
        if (entry.entryId != nil) {
            [_entries setObject:entry forKey:entry.entryId];
        }
    }
    
    [_contentTriggerObserver removeAllEntries];
    
    [self saveEntries:_entries.allValues];
    
    for (CTLEntry* entry in triggers) {        
        CTLTermParams* params = entry.termParams;
        NSDate* nextEndDateTime = params.nextEndDateTime;
        
        if (nextEndDateTime) {
            [_contentTriggerObserver addTriggerEntry:entry];
        }
    }
    
    [_contentTriggerObserver verificationAndUpdateLocalNotification];
}

-(void)setEnabledLOTriggerType:(NSString*)triggerType enabled:(BOOL)enabled{
    [_contentTriggerObserver setEnabledLOTriggerType:triggerType enabled:enabled];
}

-(void)nextExecuteQuery:(id)group query:(id)query block:(CTLFetchCompletionHandler)block resultReceiver:(NSMutableArray*)resultArray{
    [[group performSelector:@selector(bucketWithName:) withObject:kBucketName] performSelector:@selector(executeQuery:withBlock:) withObject:query withObject:^(id query, id bucket, NSArray *results, id nextQuery, NSError *error) {
        if (error) {
            block(nil, error);
            return;
        }
        
        if (results) {
            [resultArray addObjectsFromArray:results];
        }
        
        if (nextQuery) {
            [self nextExecuteQuery:group query:nextQuery block:block resultReceiver:resultArray];
        }else{
            
            NSMutableArray* entries = [NSMutableArray array];
            for (KiiObject* kiiObject in resultArray) {
                CTLEntry* entry = [[CTLEntry alloc] initWithKiiObject:kiiObject];
                if (entry) {
                    [entries addObject:entry];
                }
            }
            
            block(entries, nil);
            return;
        }
    }];
}

-(void)queryQRString:(NSString *)qrStr{
    [_contentTriggerObserver queryQRString:qrStr];
}

-(void)loadContentWithObserveResult:(CTLObserveResult*)result block:(CTLRequestContentCompletionHandler)block{
//    NSString* userId = [KiiUser currentUser].userID;
    NSString* userId = [[NSClassFromString(@"KiiUser") performSelector:@selector(currentUser)] performSelector:@selector(userID)];
    NSString* masterId = result.observeEntry.masterId;
    
//    KiiServerCodeEntry* entry = [Kii serverCodeEntry:@"master"];
    id entry = [NSClassFromString(@"Kii") performSelector:@selector(serverCodeEntry:) withObject:@"master"];
    
    NSDictionary* rawArgs = [NSDictionary dictionaryWithObjectsAndKeys:masterId, @"contentID", userId, @"userID", nil];
//    KiiServerCodeEntryArgument* args = [KiiServerCodeEntryArgument argumentWithDictionary:rawArgs];
    id args = [NSClassFromString(@"KiiServerCodeEntryArgument") performSelector:@selector(argumentWithDictionary:) withObject:rawArgs];
    
//    [entry execute:args withBlock:^(KiiServerCodeEntry *entry, KiiServerCodeEntryArgument *argument, KiiServerCodeExecResult *execResult, NSError *error) {
//        NSDictionary* returnedValue = [[execResult returnedValue] objectForKey:@"returnedValue"];
//        block(result.observeEntry, returnedValue, error);
//    }];
    [entry performSelector:@selector(execute:withBlock:) withObject:args withObject:^(id entry, id argument, id execResult, NSError *error) {
        NSDictionary* returnedValue = [[execResult returnedValue] objectForKey:@"returnedValue"];
        block(result.observeEntry, returnedValue, error);
    }];
}

#pragma mark - persistents.

+(NSArray*)entriesFromUserDefaults{
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    NSArray* entries = [NSKeyedUnarchiver unarchiveObjectWithData:[userDefaults objectForKey:kUserDefaultsKey]]; //ContentTriggerEntries.
    
    NSMutableArray* tmpArray = [NSMutableArray array];
    for (CTLEntry* entry in entries) {
        if (entry.entry != nil) {
            [tmpArray addObject:entry];
        }
    }
    return tmpArray;
}

-(void)saveEntries:(NSArray*)entries{
    NSUserDefaults* userDefaults = [NSUserDefaults standardUserDefaults];
    [userDefaults setObject:[NSKeyedArchiver archivedDataWithRootObject:entries] forKey:kUserDefaultsKey];   //ContentTriggerEntries
    [userDefaults synchronize];
}

-(void)loadEntriesAtLastTime{
    [self updateTriggers:[CTLContentTrigger entriesFromUserDefaults]];
}

#pragma mark - Methods to convert the notification.

-(CTLObserveResult*)observerResultFromUserInfo:(NSDictionary*)userInfo{
    CTLObserveResult* result = [NSKeyedUnarchiver unarchiveObjectWithData:[userInfo objectForKey:@"result"]];
    
    CTLTermParams* params = result.observeEntry.termParams;
    NSDate* date = [NSDate date];
    if (([date compare:params.prevStartDateTime] == NSOrderedDescending) && ([date compare:params.nextEndDateTime] == NSOrderedAscending)) {
        return result;
    }
    
    return nil;
}

+(UILocalNotification*)notificationWithResult:(CTLObserveResult*)result{
    NSDictionary* userInfo = @{@"result":[NSKeyedArchiver archivedDataWithRootObject:result]};
    
    UILocalNotification* notification = [UILocalNotification new];
    if ([notification respondsToSelector:@selector(setAlertTitle:)]) {
        notification.alertTitle = result.observeEntry.title;
    }
    notification.alertBody = result.observeEntry.entryDescription;
    if (notification.alertBody == nil) {
        notification.alertBody = @" ";
    }
    notification.userInfo = userInfo;
    return notification;
}

-(void)didBackgroundFetch{
    [_contentTriggerObserver verificationAndUpdateLocalNotification];
}

#pragma mark - ContentTriggerObserverCallback

-(void)contentTriggerObserver:(CTLObserver *)contentTriggerObserver observeTrigger:(CTLObserveResult *)result{
    [[UIApplication sharedApplication] presentLocalNotificationNow:[CTLContentTrigger notificationWithResult:result]];
}

@end
