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

  CMS.Util.FileLoader = function FileLoader () {
    var self = this;

    self.set = function set (file_form) {
      var deffered = $.Deferred();

      if (!file_form) {
        self.blob = "";
        return deffered.resolve();
      }

      var file_list = file_form.files;
      var file = getFirstFile(file_list);
      if(!file) return deffered.resolve();

      fileToBlob(file)
      .then(
        function success (blob) {
          self.blob = blob;
          deffered.resolve();
        },
        function error () {
          deffered.reject();
        }
      );

      return deffered.promise();
    };

    function getFirstFile (file_list) {
      if(!file_list) return;

      // 0 番目の File オブジェクトを取得
      return file_list[0];
    }

    function fileToBlob (file) {
      var deferred = $.Deferred();
      // ------------------------------------------------------------
      // FileReader オブジェクトを生成
      // ------------------------------------------------------------
      var file_reader = new FileReader();

      // ------------------------------------------------------------
      // 読み込み成功時に実行されるイベント
      // ------------------------------------------------------------
      file_reader.onload = function () {
        var ary_u8 = new Uint8Array(file_reader.result);

        //blob作成
        var blob = new Blob([ary_u8], {type: "image/jpeg"});
        deferred.resolve(blob);
      };

      // ------------------------------------------------------------
      // 読み込みを開始する（ArrayBuffer オブジェクトを得る）
      // ------------------------------------------------------------
      file_reader.readAsArrayBuffer(file);
      return deferred.promise();
    }

    self.upload = function upload (saved_kiiobj) {
      var defferd = $.Deferred();

      if (!self.blob) return defferd.resolve();
      saved_kiiobj.uploadBody(self.blob, {
        success: function () {
          defferd.resolve();
        },
        failure: function (obj, anErrorString) {
          alert('ファイルの保存に失敗しました。');
          defferd.reject();
        }
      });

      return defferd.promise();
    };

    self.download = function download (kiiobj) {
      var deffered = $.Deferred();

      kiiobj.downloadBody({
        success: function(obj, bodyBlob) {
          self.blob = bodyBlob;
          deffered.resolve(bodyBlob);
        },
        failure: function(obj, anErrorString) {
          if (anErrorString.match(/OBJECT_BODY_NOT_FOUND/)) {
            return deffered.resolve();
          } else if (anErrorString.match(/OBJECT_NOT_FOUND/)) {
            return deffered.resolve();
          }
          return deffered.reject(anErrorString);
        }
      });

      return deffered.promise();
    };

    self.url = function url () {
      if (!self.blob) return;
      return window.URL.createObjectURL(self.blob);
    };
  };
})();