//
//  AppDelegate.m
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

#import "AppDelegate.h"
#import <KiiSDK/Kii.h>

@interface AppDelegate ()
@end

@implementation AppDelegate
NSString* const kAppId = @"";
NSString* const kAppKey = @"";

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    //For update time trigger.
    [application setMinimumBackgroundFetchInterval:UIApplicationBackgroundFetchIntervalMinimum];
    
    //Init callback method.
    _callback = ^(CTLEntry* entry, NSDictionary* result, NSError *error) {
        if (error) {
            [[[UIAlertView alloc] initWithTitle:@"エラー" message:[error localizedDescription] delegate:nil cancelButtonTitle:nil otherButtonTitles:@"close", nil] show];
            return;
        }
        
        NSString* message = [AppDelegate stringFromDictionary:result];
        
        [[[UIAlertView alloc] initWithTitle:entry.title message:message delegate:nil cancelButtonTitle:nil otherButtonTitles:@"close", nil] show];
    };
    
    [Kii beginWithID:kAppId andKey:kAppKey andSite:kiiSiteJP];
    
    //Setup notification.
    if ([application respondsToSelector:@selector(registerUserNotificationSettings:)]) {
        UIUserNotificationSettings* notificationSettings = [UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeBadge | UIUserNotificationTypeSound | UIUserNotificationTypeAlert categories:nil];
        [application registerUserNotificationSettings:notificationSettings];
        [application unregisterForRemoteNotifications];
        [application registerForRemoteNotifications];
    }else{
        [application unregisterForRemoteNotifications];
        [application registerForRemoteNotificationTypes:UIRemoteNotificationTypeBadge | UIRemoteNotificationTypeSound | UIRemoteNotificationTypeAlert];
    }
    
    return YES;
}

+(NSString*)stringFromDictionary:(NSDictionary*)dict{
    
    NSString* message = @"";
    
    //Other trigger infomation. (ex. Beacon/Geofence parameters.)
    for (id key in [dict keyEnumerator]) {
        message = [message stringByAppendingString:@"\n"];
        message = [message stringByAppendingString:key];
        message = [message stringByAppendingString:@" : "];
        
        id val = [dict objectForKey:key];
        if ([val isKindOfClass:[NSNumber class]]) {
            message = [message stringByAppendingString:((NSNumber*)val).stringValue];
        }else if([val isKindOfClass:[NSDictionary class]]){
            message = [message stringByAppendingString:@"{"];
            message = [message stringByAppendingString:[AppDelegate stringFromDictionary:val]];
            message = [message stringByAppendingString:@"}"];
        }else{
            message = [message stringByAppendingString:val];
        }
    }
    
    return message;
}

-(void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken{
    _deviceToken = deviceToken;
}

//Call on tapped notification. (after background)
-(void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification{
    CTLObserveResult* result = [[CTLContentTrigger sharedInstance] observerResultFromUserInfo:notification.userInfo];
    [[CTLContentTrigger sharedInstance] loadContentWithObserveResult:result block:_callback];
}

-(void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo{
    KiiPushMessage* message = [KiiPushMessage messageFromAPNS:userInfo];
    NSString* body = [AppDelegate stringFromDictionary:message.rawMessage];
    
    [[[UIAlertView alloc] initWithTitle:@"プッシュ通知" message:body delegate:nil cancelButtonTitle:nil otherButtonTitles:@"close", nil] show];
}

-(void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    KiiPushMessage* message = [KiiPushMessage messageFromAPNS:userInfo];
    NSString* body = [AppDelegate stringFromDictionary:message.rawMessage];
    
    [[[UIAlertView alloc] initWithTitle:@"プッシュ通知" message:body delegate:nil cancelButtonTitle:nil otherButtonTitles:@"close", nil] show];
    completionHandler(UIBackgroundFetchResultNoData);
}

//Update time trigger on iOS7 or later.
-(void)application:(UIApplication *)application performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler{
    [[CTLContentTrigger sharedInstance] didBackgroundFetch];
}

@end
