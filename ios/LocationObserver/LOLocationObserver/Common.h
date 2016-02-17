//
//  Common.h
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

#ifndef LOLocationObserver_Common_h
#define LOLocationObserver_Common_h

#define FobiddneCallMethod() [NSException raise:NSInternalInconsistencyException format:@"You should not call %@ in a this class.", NSStringFromSelector(_cmd)];
#define UndefineMethod() [NSException raise:NSInternalInconsistencyException format:@"You must override %@ in a subclass.",NSStringFromSelector(_cmd)];

#endif
