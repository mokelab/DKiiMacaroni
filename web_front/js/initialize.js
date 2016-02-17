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

  CMS.Initialize = function (buckets_arr) {
    var buckets = [];
    if (!check_bucket_id_duplication(buckets_arr)) {
      var message = '重複したIDが設定されています。';
      _.each(buckets_arr, function (bucket) {
        message += '\n' + bucket.id + ': ' + bucket.label;
      })
      alert(message);
      return false;
    }
    _(buckets_arr).forEach(function (bucket) {
      var collection = new CMS.Collections.BucketBase ();
      collection.id = bucket.id;
      collection.name = bucket.bucket;
      collection.label = bucket.label;
      collection.fields = merge_fields(bucket.fields);
      collection.descriptionKeys = bucket.descriptionKeys;
      collection.editable = (bucket.editable === undefined) ? true : bucket.editable;
      collection.visible = (bucket.visible === undefined) ? true : bucket.visible;
      collection.enableImageFile = bucket.enableImageFile;
      collection.query = bucket.query;
      collection.sortKey = bucket.sortKey;
      collection.sortOrder = bucket.sortOrder;
      buckets.push(collection);
    });

    CMS.Buckets = buckets;
  };

  function check_bucket_id_duplication (buckets_arr) {
    return _.uniq(_.pluck(buckets_arr, 'id')).length === buckets_arr.length;
  }

  function merge_fields (fields) {
//    if (CMS.Config.forceAclControl) {
//      _.each(CMS.Config.aclField, function (f) {
//        var exist_field = _.findWhere(fields, {key: f.key});
//        if (!exist_field) {
//          fields = fields.concat(f);
//        }
//      });
//    }

    return fields;
  }

  CMS.InitializeContentTrigger = function (content_fields, extend_form) {
    content_fields = content_fields || [];
    extend_form = extend_form || [];

    var triggers = [geo, beacon, wifi, qr, time];
    _.each(triggers, function (trigger) {
      trigger.bucket = CMS.Config.contentTrigerBucketName;
      trigger.fields = trigger.fields.concat(content_fields);
    });

    CMS.Initialize(triggers.concat(extend_form));
  };


  var common_fields = [
      {
        "key" : "title",
        "label" : "タイトル",
        "type" : "String",
        "required" : true,
        "validator" : function (val, fields) {
          if((!(fields.sunday && fields.monday && fields.tuesday && fields.wednesday && fields.thursday && fields.friday && fields.saturday) || fields.dayOfMonth) && !(fields.startTime != undefined && fields.endTime != undefined)){
            return "曜日/日付 を絞り込む場合は、時間による絞り込みが必須です。";
          }else if((!(fields.sunday || fields.monday || fields.tuesday || fields.wednesday || fields.thursday || fields.friday || fields.saturday)) && (fields.startTime != undefined || fields.endTime != undefined)){
            return "いずれかの曜日を有効にする必要があります。";
          }
        }
      }, {
        "key" : "enabled",
        "label" : "有効",
        "type" : "Boolean",
        "defaultValue": true,
        "required" : true
      }, {
        "key" : "startDateTime",
        "label" : "有効日時 (開始)",
        "type" : "Date",
        "required" : true
      }, {
        "key" : "endDateTime",
        "label" : "有効日時 (終了)",
        "type" : "Date",
        "required" : true
      }, {
        "key" : "startTime",
        "label" : "時刻での絞り込み (開始)",
        "type" : "Time",
        "validator" : function (val, fields) {
          if (val) {
            if (!fields.endTime) {
              return "終了時刻を入力して下さい。";
            }
          }
        }
      }, {
        "key" : "endTime",
        "label" : "時刻での絞り込み (終了)",
        "type" : "Time",
        "validator" : function (val, fields) {
          if (val) {
            if (!fields.startTime) {
              return "開始時刻を入力して下さい。";
            }
          }
        }
      }, {
        "key" : "sunday",
        "label" : "指定時刻の日曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "monday",
        "label" : "指定時刻の月曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "tuesday",
        "label" : "指定時刻の火曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "wednesday",
        "label" : "指定時刻の水曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "thursday",
        "label" : "指定時刻の木曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "friday",
        "label" : "指定時刻の金曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "saturday",
        "label" : "指定時刻の土曜日",
        "type" : "Boolean",
        "defaultValue" : true
      }, {
        "key" : "dayOfMonth",
        "label" : "日付での絞り込み",
        "type" : "Number",
        "validator" : function (val, fields) {
          if (val < 1 || val > 31){
            return "日付での絞り込みは 1~31 の間だけ有効です。";
          }
        }
      }, {
        "key" : "accessLimitHour",
        "label" : "アクセス数カウント時間(Hour)",
        "type" : "Number",
        "defaultValue": 24,
        "required" : true
      }, {
        "key" : "accessLimitCount",
        "label" : "アクセス上限(0:無効)",
        "type" : "Number",
        "defaultValue": 0,
        "required" : true
      }
    ];
  
    var region_common_fields = [
      {
        "key" : "transitionEnter",
        "label" : "進入の検知",
        "type" : "Boolean",
        "defaultValue": true,
        "required" : true
      }, {
        "key" : "transitionExit",
        "label" : "退出の検知",
        "type" : "Boolean",
        "defaultValue": true,
        "required" : true
      }
    ];
  
    var geo = {
      "id": 1,
      "bucket" : "",
      "label" : "ジオフェンス",
      "descriptionKeys" : ["title", "startDateTime", 'endDateTime'],
      "query": KiiClause.equals("triggerType", "geo"),
      "enableImageFile": true,
      "fields": common_fields.concat(region_common_fields).concat([
        {
          "key" : "triggerType",
          "label" : "タイプ",
          "type" : "String",
          "required" : true,
          "defaultValue": "geo",
          "hidden": true
        }, {
          "key" : "geoPoint",
          "label" : "ジオフェンスの経度緯度",
          "type" : "GeoPoint",
          "required" : true
        }, {
          "key" : "range",
          "label" : "ジオフェンスの半径(m)",
          "type" : "Number",
          "required" : true,
          "validator" : function (val) {
            if (val < 100 || val > 10000) {
              return "半径は100〜10000を入力してください。";
            }
          }
        }
      ])
    };

    var beacon = {
      "id": 2,
      "bucket" : "",
      "label" : "iBeacon",
      "descriptionKeys" : ["title", "startDateTime", 'endDateTime'],
      "query": KiiClause.equals("triggerType", "beacon"),
      "enableImageFile": true,
      "fields": common_fields.concat(region_common_fields).concat([
        {
          "key" : "triggerType",
          "label" : "タイプ",
          "type" : "String",
          "required" : true,
          "defaultValue": "beacon",
          "hidden": true
        }, {
          "key" : "peripheralUUID",
          "label" : "iBeaconのUUID",
          "type" : "String",
          "required" : true,
          "validator" : function (val) {
            if (val) {
              if (!val.match(/^[0-9A-Za-z]{8}-[0-9A-Za-z]{4}-[0-9A-Za-z]{4}-[0-9A-Za-z]{4}-[0-9A-Za-z]{12}$/)) {
                return "UUIDは正しい書式で入力して下さい(XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX)";
              }
            }
          }
        }, {
          "key" : "major",
          "label" : "iBeaconのMajor",
          "type" : "Number",
          "validator" : function (val, fields) {
            if (val !== undefined) {
              if (val < 0 || val > 65535) {
                return "major は 0から65535 の間で有効です";
              }
            }
          }
        }, {
          "key" : "minor",
          "label" : "iBeaconのMinor",
          "type" : "Number",
          "validator" : function (val, fields) {
            if (val !== undefined) {
              if (val < 0 || val > 65535) {
                return "minor は 0から65535 の間で有効です";
              }
            }
          }
        }, {
          "key" : "RSSI",
          "label" : "iBeaconを検知するRSSI強度",
          "type" : "Number"
        }
      ])
    };

    var wifi = {
      "id": 3,
      "bucket" : "",
      "label" : "wifi",
      "descriptionKeys" : ["title", "startDateTime", 'endDateTime'],
      "query": KiiClause.equals("triggerType", "wifi"),
      "enableImageFile": true,
      "fields": common_fields.concat(region_common_fields).concat([
        {
          "key" : "triggerType",
          "label" : "タイプ",
          "type" : "String",
          "required" : true,
          "defaultValue": "wifi",
          "hidden": true
        }, {
          "key" : "SSID",
          "label" : "アクセスポイントのSSID",
          "type" : "String",
          "required" : true
        }, {
          "key" : "BSSID",
          "label" : "アクセスポイントのBSSID",
          "type" : "String"
        }
      ])
    };

    var qr = {
      "id": 4,
      "bucket" : "",
      "label" : "qr",
      "descriptionKeys" : ["title", "startDateTime", 'endDateTime'],
      "query": KiiClause.equals("triggerType", "qr"),
      "enableImageFile": true,
      "fields": common_fields.concat([
        {
          "key" : "triggerType",
          "label" : "タイプ",
          "type" : "String",
          "required" : true,
          "defaultValue": "qr",
          "hidden": true
        }, {
          "key" : "target",
          "label" : "テキストQRコード",
          "type" : "String",
          "required" : true
        }
      ])
    };

    var time = {
      "id": 5,
      "bucket" : "",
      "label" : "時刻",
      "descriptionKeys" : ["title", "startDateTime", 'endDateTime'],
      "query": KiiClause.equals("triggerType", "time"),
      "enableImageFile": true,
      "fields": common_fields.concat([
        {
          "key" : "triggerType",
          "label" : "タイプ",
          "type" : "String",
          "required" : true,
          "defaultValue": "time",
          "hidden": true
        }
      ])
    };
})();