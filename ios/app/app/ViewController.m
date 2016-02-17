//
//  ViewController.m
//  app
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

#import "ViewController.h"
#import "AppDelegate.h"
#import <ContentTriggerLibrary/ContentTriggerLibrary.h>
#import <KiiSDK/Kii.h>

@interface ViewController ()
@property BOOL pushLoaded;
@end

@implementation ViewController
NSString* const kGroupId = @"0000000000000000000000000";

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _pushLoaded = NO;
    
    NSError* error;
    [KiiUser authenticateSynchronous:_userName withPassword:_password andError:&error];
    if (error) {
        UIAlertView* alert = [[UIAlertView alloc] initWithTitle:@"ログイン失敗" message:@"" delegate:nil cancelButtonTitle:nil otherButtonTitles:@"閉じる", nil];
        [alert show];
    }else{
        //(1). Initialze library.
        CTLContentTrigger* contentTrigger = [CTLContentTrigger sharedInstance];
        contentTrigger.groupId = kGroupId; //GroupId
        
        [contentTrigger loadEntriesAtLastTime];
        
        _updateButton.enabled = YES;
        _qrButton.enabled = YES;
        
        AppDelegate* delegate = [UIApplication sharedApplication].delegate;
        
        if (delegate.deviceToken) {
            [self registerDevice];
        }else{
            [delegate addObserver:self forKeyPath:@"deviceToken" options:NSKeyValueObservingOptionNew context:nil];
        }
    }
}

-(void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context{
    if ([keyPath isEqual:@"deviceToken"]) {
        [self registerDevice];
    }
}

-(void)registerDevice{
    AppDelegate* delegate = [UIApplication sharedApplication].delegate;
    [KiiPushInstallation installWithDeviceToken:delegate.deviceToken andDevelopmentMode:YES andCompletion:^(KiiPushInstallation *installation, NSError *error) {
        if (error) {
            UIAlertView* alert = [[UIAlertView alloc] initWithTitle:@"APNS登録失敗" message:@"" delegate:nil cancelButtonTitle:nil otherButtonTitles:@"閉じる", nil];
            [alert show];
        }
    }];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (IBAction)onClickQr:(id)sender {
    CTLContentTrigger* contentTrigger = [CTLContentTrigger sharedInstance];
    
    //(ex) Test trigger of QR.
    [contentTrigger queryQRString:@"http://www.up-frontier.jp/"];
}

- (IBAction)onClickUpdate:(id)sender {
    CTLContentTrigger* contentTrigger = [CTLContentTrigger sharedInstance];
    
    //(2). Fetch triggers.
    [contentTrigger fetchWithBlock:^(NSArray* triggers, NSError *error) {
        if (error) {
            _output.text = @"Update failed.";
            //Error.
            return;
        }
        
        //(optional). Customize triggers.
        
        //(3). Update triggers.
        [contentTrigger updateTriggers:triggers];
        
        _output.text = @"Update was successful.";
        
        
        if (!_pushLoaded) {
            [self performSelectorInBackground:@selector(checkPushLoaded) withObject:nil];
        }
    }];
}

-(void)checkPushLoaded{
    BOOL maleIsSubscribed = NO;
    BOOL femaleIsSubscribed = NO;
    
    [self checkPushLoadedWithResult:&maleIsSubscribed withTopicName:@"male"];
    [self checkPushLoadedWithResult:&femaleIsSubscribed withTopicName:@"female"];
    
    _isMale.on = maleIsSubscribed;
    _isFemale.on = femaleIsSubscribed;
    
    _isMale.enabled = YES;
    _isFemale.enabled = YES;
    
    _pushLoaded = YES;
}

-(void)checkPushLoadedWithResult:(BOOL*)result withTopicName:(NSString*)topicName{
    NSError* error = nil;
    KiiTopic* topic = [Kii topicWithName:topicName];
    BOOL res = [KiiPushSubscription checkSubscriptionSynchronous:topic withError:&error];
    *result = res;
}

- (IBAction)onChangeMaleValue:(id)sender {
    [self changeSubscribeWithSwitch:_isMale withTopicName:@"male"];
}

- (IBAction)onChangeFemaleValue:(id)sender {
    [self changeSubscribeWithSwitch:_isFemale withTopicName:@"female"];
}

-(void)changeSubscribeWithSwitch:(UISwitch*)uiSwitch withTopicName:(NSString*)topicName{
    KiiTopic* topic = [Kii topicWithName:topicName];
    if (uiSwitch.on) {
        [KiiPushSubscription subscribe:topic withBlock:^(KiiPushSubscription *subscription, NSError *error) {
            if (error || subscription == nil) {
                uiSwitch.on = false;
                _output.text = [[@"Subscribe '" stringByAppendingString:topicName] stringByAppendingString:@"' failed."];
            }else{
                _output.text = [[@"Subscribe '" stringByAppendingString:topicName] stringByAppendingString:@"' was successful."];
            }
        }];
    }else{
        [KiiPushSubscription unsubscribe:topic withBlock:^(KiiPushSubscription *subscription, NSError *error) {
            if (error || subscription == nil) {
                uiSwitch.on = true;
                _output.text = [[@"Unsubscribe '" stringByAppendingString:topicName] stringByAppendingString:@"' failed."];
            }else{
                _output.text = [[@"Unsubscribe '" stringByAppendingString:topicName] stringByAppendingString:@"' was successful."];
            }
        }];
    }
}

@end
