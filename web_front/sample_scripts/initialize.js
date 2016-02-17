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

CMS.Initialize([
  {
    "name" : "news",
    "label" : "お知らせ",
    "fields" : [
      {
        "key" : "title",
        "label" : "タイトル",
        "type" : "String",
        "required" : true,
        "defaultValue" : "お知らせです"
      },{
        "key" : "message",
        "label" : "メッセージ",
        "type" : "String",
        "required" : true
      },{
        "key": "limit",
        "label": "制限回数",
        "type": "Number",
      }, {
        "key" : "geo",
        "label" : "ポジション",
        "type" : "GeoPoint",
        "required" : true,
        "defaultValue" : "お知らせです"
      }, {
        "key" : "ref",
        "label" : "参照",
        "type" : "Reference",
        "required" : true,
        "defaultValue" : "お知らせです"
      }, {
        "key" : "arr",
        "label" : "配列テスト",
        "type" : "Array",
        "required" : true,
        "defaultValue" : "お知らせです",
        "fields": [
          {
            "key": "label1",
            "label": "ラベル",
            "type": "String",
            "required" : true,
            "defaultValue" : "aaaa",
          },
          {
            "key": "endDate",
            "label": "終了日",
            "type": "Date"
          }
        ]
       }
     ],
    "descriptionKeys" : ["title", "geo", 'uuid']
  }, {
    "name" : "content",
    "label" : "コンテンツ",
    "fields" : [
      {
        "key" : "title",
        "label" : "タイトル",
        "type" : "String",
        "required" : true,
        "defaultValue" : "お知らせです"
      },{
        "key" : "description",
        "label" : "内容",
        "type" : "String",
        "required" : true
      }, {
        "key" : "publishDate",
        "label" : "配信時刻",
        "type" : "Date",
        "required" : true
      }
    ],
    "descriptionKeys" : ["title", "publishDate"],
    "editable": false
  }
]);
