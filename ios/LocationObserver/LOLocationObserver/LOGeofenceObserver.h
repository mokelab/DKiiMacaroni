//
//  LOGeofenceObserver.h
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

#import "LOLocationObserver.h"

#define _LO_KEY_LATITUDE @"latitude"
#define _LO_KEY_LONGITUDE @"longitude"
#define _LO_KEY_RADIUS @"range"

static NSString* const kKeyLatitude = _LO_KEY_LATITUDE;
static NSString* const kKeyLongitude = _LO_KEY_LONGITUDE;
static NSString* const kKeyRange = _LO_KEY_RADIUS;

@interface LOGeofenceEntryBuilder : LOEntryBuilder
-(instancetype)initWithEntryId:(NSString *)entryId enter:(BOOL)enter exit:(BOOL)exit latitude:(double)latitude longitude:(double)longitude range:(double)range;
-(NSDictionary*) create;
@end

/**
 Geofence の監視クラス
 */
@interface LOGeofenceObserver : LOLocationObserver
@end
