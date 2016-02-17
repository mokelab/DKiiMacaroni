//
//  ContentTriggerEntry.m
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

#import "CTLContentTrigger.h"
#import "CTLEntry.h"
#import <LOLocationObserver/LOLocationObserver.h>
#import <LOLocationObserver/LOBLEObserver.h>
#import <LOLocationObserver/LOGeofenceObserver.h>

@class KiiObject;

NSString* const kTriggerTypeBLE = @"beacon";
NSString* const kTriggerTypeGeofence = @"geo";
NSString* const kTriggerTypeQR = @"qr";
NSString* const kTriggerTypeTime = @"time";

static NSString* const kTitle = @"title";
static NSString* const kDescription = @"description";
static NSString* const kMasterId = @"relation";
static NSString* const kLOTriggerType = @"triggerType";
static NSString* const kStartDateTime = @"startDateTime";
static NSString* const kEndDateTime = @"endDateTime";
static NSString* const kStartTime = @"startTime";
static NSString* const kEndTime = @"endTime";
static NSString* const kSunday = @"sunday";
static NSString* const kMonday = @"monday";
static NSString* const kTuesday = @"tuesday";
static NSString* const kWednesday = @"wednesday";
static NSString* const kThursday = @"thursday";
static NSString* const kFriday = @"friday";
static NSString* const kSaturday = @"saturday";
static NSString* const kDayOfMonth = @"dayOfMonth";
static NSString* const kTransitionEnter = @"transitionEnter";
static NSString* const kTransitionExit = @"transitionExit";

NSString* const kCTLContentTriggerKeyLatitude = _LO_KEY_LATITUDE;
NSString* const kCTLContentTriggerKeyLongitude = _LO_KEY_LONGITUDE;
NSString* const kCTLContentTriggerKeyRadius = _LO_KEY_RADIUS;

NSString* const kCTLContentTriggerKeyProximityUUID = _LO_KEY_UUID;
NSString* const kCTLContentTriggerKeyMajorId = _LO_KEY_MAJOR_ID;
NSString* const kCTLContentTriggerKeyMinorId = _LO_KEY_MINOR_ID;
NSString* const kCTLContentTriggerKeyRSSI = _LO_KEY_RSSI;

@interface DateConverter : NSObject
@end
@implementation DateConverter
+(NSDate*)dateWithTime:(NSString*)time baseDate:(NSDate*)baseDate inFuture:(BOOL)inFuture{
    if (!([time length] > 0)) {
        return nil;
    }
    
    NSDateFormatter* formatter = [[NSDateFormatter alloc] init];
    formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:@"ja"];
    formatter.dateFormat = @"kk:mm";
    formatter.timeZone = [NSTimeZone timeZoneWithName:@"Asia/Tokyo"];
    NSDate* date = [formatter dateFromString:time];
    
    NSCalendar* calendar = [NSCalendar currentCalendar];
    NSDateComponents* dateComps = [calendar components:NSYearCalendarUnit | NSMonthCalendarUnit | NSDayCalendarUnit | NSHourCalendarUnit | NSMinuteCalendarUnit | NSSecondCalendarUnit fromDate:date];
    NSDateComponents* todayComps = [calendar components:NSYearCalendarUnit | NSMonthCalendarUnit | NSDayCalendarUnit | NSHourCalendarUnit | NSMinuteCalendarUnit | NSSecondCalendarUnit fromDate:baseDate];
    
    todayComps.hour = dateComps.hour;
    todayComps.minute = dateComps.minute;
    todayComps.second = dateComps.second;
    todayComps.nanosecond = dateComps.nanosecond;
    date = [calendar dateFromComponents:todayComps];
    
    int order = [date compare:baseDate];
    int aDay = 0;
    if (inFuture && order == NSOrderedAscending) {
        aDay = 1;
    }else if(!inFuture && order == NSOrderedDescending){
        aDay = -1;
    }
    
    if (aDay != 0) {
        NSDateComponents* aDayComp = [NSDateComponents new];
        aDayComp.day = aDay;
        
        date = [[NSCalendar currentCalendar] dateByAddingComponents:aDayComp toDate:date options:0];
    }
    
    return date;
}
@end

@interface CTLTermParams()<NSCoding>
@end

@implementation CTLTermParams

const int kSundayFlag = 0x0001;
const int kMondayFlag = 0x0002;
const int kTuesdayFlag = 0x0004;
const int kWednesdayFlag = 0x0008;
const int kThursdayFlag = 0x0010;
const int kFridayFlag = 0x0020;
const int kSaturdayFlag = 0x0040;

-(instancetype)initWithKiiObject:(id)kiiObject{
    self = [super init];
    if (self) {
        _startDateTime = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kStartDateTime];
        _endDateTime = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kEndDateTime];
        _startTime = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kStartTime];
        _endTime = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kEndTime];
        NSDictionary* dayOfWeeks = @{kSunday:@(kSundayFlag),
                                     kMonday:@(kMondayFlag),
                                     kTuesday:@(kTuesdayFlag),
                                     kWednesday:@(kWednesdayFlag),
                                     kThursday:@(kThursdayFlag),
                                     kFriday:@(kFridayFlag),
                                     kSaturday:@(kSaturdayFlag)};
        for (NSString* dayOfWeek in dayOfWeeks.allKeys) {
            BOOL boolObj = true;
            if ([kiiObject performSelector:@selector(hasObject:) withObject:dayOfWeek]) {
                boolObj = [[kiiObject performSelector:@selector(getObjectForKey:) withObject:dayOfWeek] boolValue];
            }
            if (boolObj) {
                _dayOfWeekFlags |= [[dayOfWeeks objectForKey:dayOfWeek] intValue];
            }
        }
        _dayOfMonth = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kDayOfMonth];
    }
    return self;
}

-(void)encodeWithCoder:(NSCoder *)aCoder{
    @try {
        [aCoder encodeObject:_startTime];
        [aCoder encodeObject:_endTime];
        [aCoder encodeObject:_startDateTime];
        [aCoder encodeObject:_endDateTime];
        [aCoder encodeObject:_dayOfMonth];
        [aCoder encodeInteger:_dayOfWeekFlags forKey:@"_dayOfWeekFlags"];
    }
    @catch (NSException *exception) {
    }
}

-(id)initWithCoder:(NSCoder *)aDecoder{
    self = [super init];
    if (self) {
        @try {
            _startTime = [aDecoder decodeObject];
            _endTime = [aDecoder decodeObject];
            _startDateTime = [aDecoder decodeObject];
            _endDateTime = [aDecoder decodeObject];
            _dayOfMonth = [aDecoder decodeObject];
            _dayOfWeekFlags = [aDecoder decodeIntegerForKey:@"_dayOfWeekFlags"];
        }
        @catch (NSException *exception) {
            return nil;
        }
    }
    return self;
}

-(NSDate*)nextStartDateTime{
    return [self searchDateTimeForFuture:YES isStart:YES];
}
-(NSDate*)nextEndDateTime{
    return [self searchDateTimeForFuture:YES isStart:NO];
}
-(NSDate*)prevStartDateTime{
    return [self searchDateTimeForFuture:NO isStart:YES];
}
-(NSDate*)prevEndDateTime{
    return [self searchDateTimeForFuture:NO isStart:NO];
}

//private
-(NSDate*)searchDateTimeForFuture:(BOOL)inFuture isStart:(BOOL)isStart{
    NSDate* now = [NSDate date];
    NSDate* startDate;
    NSDate* endDate;
    
    
    if (_startDateTime > 0) {
        startDate = [[NSDate alloc] initWithTimeIntervalSince1970:_startDateTime.doubleValue/1000];
        
        if (_endDateTime > 0) {
            endDate = [[NSDate alloc] initWithTimeIntervalSince1970:_endDateTime.doubleValue/1000];
        }
        
        if ([self isSplitTerm]) {
            
            NSCalendar* calendar = [NSCalendar currentCalendar];
            startDate = [calendar dateBySettingHour:0 minute:0 second:0 ofDate:startDate options:0];
            endDate = [calendar dateBySettingHour:23 minute:59 second:59 ofDate:endDate options:0];
            
            NSDate* queryDate = [DateConverter dateWithTime:isStart ? _startTime : _endTime baseDate:now inFuture:inFuture];
            if (queryDate == nil) {
                return nil;
            }
            
            const int SEARCH_LIMIT = 1000;
            for (int i=0; i<SEARCH_LIMIT; i++) {
                BOOL isSoFar = inFuture ? endDate != nil && [queryDate compare:endDate] == NSOrderedDescending : [queryDate compare:startDate] == NSOrderedAscending;
                if (isSoFar) {
                    return nil;
                }
                
                if (inFuture ? [queryDate compare:now] == NSOrderedDescending : [queryDate compare:now] == NSOrderedAscending) {
                    if ([queryDate compare:endDate] != NSOrderedDescending && [queryDate compare:startDate] != NSOrderedAscending && [CTLTermParams isEqualDayOfMonthWithDate:queryDate dayOfMonth:_dayOfMonth] && [CTLTermParams isEqualDayOfWeekWithDate:queryDate dayOfWeekFalgs:_dayOfWeekFlags]) {
                        return queryDate;
                    }
                }
                
                NSDateComponents* aDayComponent = [NSDateComponents new];
                aDayComponent.day = inFuture ? 1 : -1;
                
                queryDate = [calendar dateByAddingComponents:aDayComponent toDate:queryDate options:0];
            }
            
            return nil;
        }else{
            NSDate* tmpDate = isStart ? startDate : endDate;
            BOOL isValid = inFuture ? [now compare:tmpDate] == NSOrderedAscending : [now compare:tmpDate] == NSOrderedDescending;
            if (isValid) {
                return tmpDate;
            }else{
                return nil;
            }
        }
    }else{
        return nil;
    }
}

-(BOOL)isSplitTerm{
    return _startTime.length > 0 && _endTime.length > 0;
}

+(BOOL)isEqualDayOfMonthWithDate:(NSDate*)date dayOfMonth:(NSNumber*)dayOfMonth{
    NSCalendar* calendar = [NSCalendar currentCalendar];
    return dayOfMonth.integerValue == 0 || [calendar component:NSDayCalendarUnit fromDate:date] == dayOfMonth.integerValue;
}

+(BOOL)isEqualDayOfWeekWithDate:(NSDate*)date dayOfWeekFalgs:(NSInteger)dayOfWeekFlags{
    NSCalendar* calendar = [NSCalendar currentCalendar];
    NSInteger weekday = [calendar components:NSWeekdayCalendarUnit fromDate:date].weekday;
    NSInteger nowDayOfWeek = 1 << (weekday - 1);
    
    return (nowDayOfWeek & dayOfWeekFlags) != 0;
}
@end

@interface CTLEntry()<NSCoding>
@end

@implementation CTLEntry
-(instancetype)init{
    return nil;
}

-(instancetype)initWithKiiObject:(id)kiiObject{
    self = [super init];
    if (self) {        
        NSString* entryId = [kiiObject performSelector:@selector(uuid)];
        _title = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kTitle];
        _entryDescription = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kDescription];
        _masterId = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kMasterId];
        BOOL transitionEnter = [[kiiObject performSelector:@selector(getObjectForKey:) withObject:kTransitionEnter] boolValue];
        BOOL transitionExit = [[kiiObject performSelector:@selector(getObjectForKey:) withObject:kTransitionExit] boolValue];
        
        _termParams = [[CTLTermParams alloc] initWithKiiObject:kiiObject];
        
        _triggerType = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kLOTriggerType];
        if ([_triggerType isEqualToString:kTriggerTypeBLE]) {
            NSString* uuid = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kCTLContentTriggerKeyProximityUUID];
            NSDecimalNumber* majorId = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kCTLContentTriggerKeyMajorId];
            NSDecimalNumber* minorId = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kCTLContentTriggerKeyMinorId];
            NSDecimalNumber* rssi = [kiiObject performSelector:@selector(getObjectForKey:) withObject:kCTLContentTriggerKeyRSSI];
            
            LOBLEEntryBuilder* builder = [[LOBLEEntryBuilder alloc] initWithEntryId:entryId enter:transitionEnter exit:transitionExit uuid:uuid];
            NSDictionary* tmpEntry = [builder createWithConfigurationBlock:^(LOBLEEntryBuilderConfiguration *config) {
                if (majorId) {
                    [config setMajorId:majorId];
                }
                if (minorId) {
                    [config setMinorId:minorId];
                }
                if (rssi) {
                    [config setRssiLevel:rssi];
                }
            }];
            _entry = tmpEntry;
            _entryId = tmpEntry[kKeyEntryId];
        }else if ([_triggerType isEqualToString:kTriggerTypeGeofence]){
            id geoPoint = [kiiObject performSelector:@selector(getGeoPointForKey:) withObject:@"geoPoint"];
            NSString* radius = [kiiObject performSelector:@selector(getObjectForKey:) withObject:@"range"];
            
            double latitude;
            double longitude;
            
            NSMethodSignature *sig = [[geoPoint class] instanceMethodSignatureForSelector:@selector(latitude)];
            NSInvocation* inv = [NSInvocation invocationWithMethodSignature:sig];
            [inv setSelector:@selector(latitude)];
            [inv setTarget:geoPoint];
            [inv invoke];
            [inv getReturnValue:&latitude];
            
            sig = [[geoPoint class] instanceMethodSignatureForSelector:@selector(longitude)];
            inv = [NSInvocation invocationWithMethodSignature:sig];
            [inv setSelector:@selector(longitude)];
            [inv setTarget:geoPoint];
            [inv invoke];
            [inv getReturnValue:&longitude];
            
            NSDictionary* tmpEntry = [[[LOGeofenceEntryBuilder alloc] initWithEntryId:entryId enter:transitionEnter exit:transitionExit latitude:latitude longitude:longitude range:radius.integerValue] create];
            _entry = tmpEntry;
            _entryId = tmpEntry[kKeyEntryId];
        }else if ([_triggerType isEqualToString:kTriggerTypeQR]){
            _entry = @{kKeyEntryId:entryId, kCTLContentTriggerKeyQrTarget:[kiiObject performSelector:@selector(getObjectForKey:) withObject:@"target"]};
            _entryId = entryId;
        }else if ([_triggerType isEqualToString:kTriggerTypeTime]){
            _entry = @{kKeyEntryId:entryId};
            _entryId = entryId;
        }else{
            return nil;
        }
    }
    return self;
}

#pragma mark - NSCoding

-(void)encodeWithCoder:(NSCoder *)aCoder{
    [aCoder encodeObject:[NSKeyedArchiver archivedDataWithRootObject:_entry]];
    [aCoder encodeObject:[NSKeyedArchiver archivedDataWithRootObject:_termParams]];
    [aCoder encodeObject:_entryId];
    [aCoder encodeObject:_title];
    [aCoder encodeObject:_masterId];
    [aCoder encodeObject:_entryDescription];
    [aCoder encodeObject:_triggerType];
}

-(id)initWithCoder:(NSCoder *)aDecoder{
    self = [super init];
    if (self)
    {
        @try {
            _entry = [NSKeyedUnarchiver unarchiveObjectWithData:[aDecoder decodeObject]];
            _termParams = [NSKeyedUnarchiver unarchiveObjectWithData:[aDecoder decodeObject]];
            _entryId = [aDecoder decodeObject];
            _title = [aDecoder decodeObject];
            _masterId = [aDecoder decodeObject];
            _entryDescription = [aDecoder decodeObject];
            _triggerType = [aDecoder decodeObject];
        }
        @catch (NSException *exception) {
            return nil;
        }
    }
    
    if (_entry == nil || _termParams == nil) {
        return nil;
    }
    return self;
}

@end
