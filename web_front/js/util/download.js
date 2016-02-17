/* Copyright 2016 Kii Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function(){
  'use strict';
  
  CMS.Util.Download = (function () {
    return {
    
      getObject: function getObject () {
        var deferred = $.Deferred();
        var group = new KiiGroup.groupWithURI(CMS.Config.groupURI);
        var bucket = group.bucketWithName(CMS.Config.scriptsBucketName);
        var queryCallbacks = {
          success: function(queryPerformed, resultSet, nextQuery) {
            deferred.resolve(resultSet);
          },
          failure: function(queryPerformed, anErrorString) {
            deferred.reject(anErrorString);
          }
        };

        bucket.executeQuery(null, queryCallbacks);
        return deferred.promise();
      },
    
      downloadBody: function downloadBody (obj) {
        var deferred = $.Deferred();

        obj.downloadBody({
          success: function (o, blob) {
            deferred.resolve(blob);
          },
          failure: function(o, anErrorString) {
            deferred.reject(anErrorString);
          }
        });

        return deferred.promise();
      },
    
      addScript: function addScript (bodyBlob) {
//      var file_reader = new FileReader();
//      file_reader.onload = function() {
//        eval(file_reader.result);
//      };
//      file_reader.readAsText(bodyBlob);
        var url = window.URL.createObjectURL(bodyBlob);
        var script = document.createElement("script");
        script.type  = "text/javascript";
        script.src = url;
        document.body.appendChild(script);
        document.body.removeChild(document.body.lastChild);
      },
    
      exec: function exec () {
        var self = this;
        var deferred = $.Deferred();

        self.getObject()

        .then(
          function (resultSet) {
            var deferred_arr = [];
            for(var i=0; i<resultSet.length; i++) {
              deferred_arr.push(self.downloadBody(resultSet[i]));
            }
            return $.when.apply(null, deferred_arr);
          }, 
          function error (e) {
            console.log(e);
          }
        )

        .then(
          function () {
            for (var i=0; i<arguments.length; i++) {
              self.addScript(arguments[i]);
            }
            setTimeout(function () {
              deferred.resolve();
            }, 100);
          }, 
          function error (e) {
            console.log(e);
            deferred.reject();
          }
        );

        return deferred.promise();
      }
    };
  })();
})();
