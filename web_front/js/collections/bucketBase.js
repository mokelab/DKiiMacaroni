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

  CMS.Collections.BucketBase = Backbone.Collection.extend({
    id: "",
    name: "",
    label: "",
    fields: [],
    descriptionKeys: [],
    editable: true,
    visible: true,
    modelName: "ObjectBase",
    enableImageFile: false,
    query: "",
    sortKey: undefined,
    sortOrder: undefined,
    perPage: 20,
    nextQuery: null,
    is_head: false,

    initialize: function () {
      _.bindAll(this, "fetch", "mapToModel");
    },

    fetch: function () {
      delete this.origModels;
      var self = this;
      var deferred = $.Deferred();
      var cache_data = [];

      this.bucket = self.getBucket();

      var callback = {
        success: function (queryPerformed, resultSet, nextQuery) {
          self.nextQuery = nextQuery;
          self.reset(self.mapToModel(resultSet));
          deferred.resolve();
        },
        failure: function(queryPerfoemed, error) {
          deferred.reject(error);
        }
      };

      var query = self.nextQuery || self.initialQuery();

      this.bucket.executeQuery(query, callback);

      return deferred.promise();
    },

    getBucket: function () {
      var group = new KiiGroup.groupWithURI(CMS.Config.groupURI);
      return group.bucketWithName(this.name);
    },

    initialQuery: function () {
      var clause = KiiClause.equals(CMS.Config.deletedFieldName, false);
      var opt_query;
      if (typeof this.query === "function") {
        opt_query = this.query();
        clause = KiiClause.and(clause, opt_query);
      } else if (this.query instanceof KiiClause) {
        opt_query = this.query;
        clause = KiiClause.and(clause, opt_query);
      }

      var sortKey = this.sortKey ? this.sortKey : '_created';
      var sortByAsc = ((typeof(this.sortOrder) == 'string') && (this.sortOrder.toLowerCase() === 'asc'));
      var query = KiiQuery.queryWithClause(clause);
      sortByAsc ? query.sortByAsc(sortKey) : query.sortByDesc(sortKey);
      query.setLimit(this.perPage);
      return query;
    },

    mapToModel: function (resultSet) {
      var self = this;
      return _.map(resultSet, function (object) {
        var attr = {
          kiiobj: object,
          fields: self.fields,
          descriptionKeys: self.descriptionKeys,
          editable: self.editable,
          enableImageFile: self.enableImageFile
        };

        // fieldsの値をkiiobjectから取得
        _.each(self.fields, function (field) {
          var type = field.type.toLowerCase();
          var key = field.key;
          if (type === "geopoint") {
            attr[key] = self.toPoint(object, key);
          } else if (type === "date") {
            attr[key] = (object.get(key) || object.get(key) === 0) ? new Date(object.get(key)) : "";
          } else {
            attr[key] = object.get(key);
          }
        });

        return new CMS.Models[self.modelName](attr);
      });
    },

    toPoint: function (object, key) {
      var point;
      var obj = object.get(key);

      if (obj) { // keyが存在しない場合getGeoPointが例外を起こすため
        var geo = object.getGeoPoint(key);
        point = {
          lat: geo.getLatitude(),
          lon: geo.getLongitude()
        };
      } else {
        point = {lat: "", lon: ""};
      }

      return point;
    },

    newObject: function () {
      var attr = {
        kiiobj: this.bucket.createObject(),
        fields: this.fields,
        descriptionKeys: this.descriptionKeys,
        editable: true,
        visible: true,
        sortKey: undefined,
        sortOrder: undefined,
        enableImageFile: this.enableImageFile,
        is_new: true
      };

      _.each(this.fields, function (field) {
        var type = field.type.toLowerCase();
        if (!(type === "reference" || type === "date" || type === "array")) {
          attr[field.key] = field.defaultValue;
        }
        if (type === "date") {
          attr[field.key] = (field.defaultValue) ? new Date(field.defaultValue) : "";
        }
      }.bind(this));

      var model = new CMS.Models[this.modelName](attr);

      return model;
    }
  });
})();
