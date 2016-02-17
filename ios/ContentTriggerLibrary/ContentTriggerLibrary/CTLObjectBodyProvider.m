//
//  ObjectBodyProvider.m
//  KiiCouldPlayground
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

#import "CTLObjectBodyProvider.h"

@interface CTLObjectBodyProvider()
@end

static const NSString* CACHE_DIR = @"object_body_cache";
@implementation CTLObjectBodyProvider

+(KiiDownloader*) downloadWithKiiObject:(id)object completion:(void(^)(UIImage* transferObject, NSError *error))completion{
    if (![[NSFileManager defaultManager] fileExistsAtPath:[self getCacheDir]]) {
        [[NSFileManager defaultManager] createDirectoryAtPath:[self getCacheDir] withIntermediateDirectories:YES attributes:nil error:nil];
    }
    
    NSString* cacheDir = [self getCacheDir];
    NSString* cacheFilePath = [[cacheDir stringByAppendingString:@"/"] stringByAppendingString:[object performSelector:@selector(getObjectForKey:) withObject:@"_id"]];
    
    id downloader = [object performSelector:@selector(downloader:) withObject:cacheFilePath];
    if (!downloader) {
        return nil;
    }
    
    [downloader performSelector:@selector(transferWithProgressBlock:andCompletionBlock:) withObject:nil withObject:^(id transferObject, NSError* error){
        if (error) {
            completion(nil, error);
        }
        NSData* data = [NSData dataWithContentsOfFile:cacheFilePath];
        UIImage* image = [UIImage imageWithData:data];
        [[NSFileManager defaultManager] removeItemAtPath:cacheFilePath error:nil];
        completion(image, nil);
    }];
    
    return downloader;
}

+(NSString*)getCacheDir{
    NSArray* dirs = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    return [dirs[0] stringByAppendingFormat:@"/%@", CACHE_DIR];
}
@end
