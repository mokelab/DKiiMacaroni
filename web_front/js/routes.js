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

//  CMS.Router = Backbone.Router.extend({
  CMS.Router = (function () {
    var viewInstance = {};
    var selectedBucketId = "";
    var tempModel = {};

    var getBucket = function getBucket (id) {
      if (id) {
        selectedBucketId = id;
      } else {
        id = selectedBucketId;
      }

      return _.find(CMS.Buckets, {'id': id});
    };

    var bucketFetchToView = function bucketFetchToView (collection) {
      collection.fetch().then(function () {
        viewInstance = new CMS.Views.BucketView({
          collection: collection
        });
        $('#app').empty().html(viewInstance.render().el);
      }, function (error) {
        alert(error);
      });
    };

    return {
      login: function login () {
        this.dispose();

        viewInstance = new CMS.Views.Login({model: new CMS.Models.LoginStatus()});
        $('#app').append(viewInstance.render().el);
      },

      buckets: function buckets () {
        this.dispose();

        viewInstance = new CMS.Views.BucketsView({
          collection: CMS.Buckets
        });
        $('#app').empty().html(viewInstance.render().el);
      },

      bucketIndex: function bucketIndex (id) {
        this.dispose();

        var collection = getBucket(id);
        collection.nextQuery = null;
        collection.is_head = true;

        bucketFetchToView(collection);
      },

      bucketIndexCurrentPage: function buketIndexNextPage (id) {
        this.dispose();

        viewInstance = new CMS.Views.BucketView({
          collection: getBucket(id)
        });
        $('#app').empty().html(viewInstance.render().el);
      },

      bucketIndexNextPage: function buketIndexNextPage (id) {
        this.dispose();

        var collection = getBucket(id);
        collection.is_head = false;

        bucketFetchToView(collection);
      },

      copyObject: function copyObject (cid) {
        var self = this;
        self.dispose();

        var collection = this.selectedBucket();
        var org = collection.get(cid);
        var attr = org.toJSON();
        attr.kiiobj = collection.bucket.createObject();
        attr.editable = true;
        attr.is_new = true;
        attr.enableImageFile = org.get('enableImageFile');

        tempModel = new CMS.Models.ObjectBase(attr);
        tempModel.set('file', $.extend(true, {}, org.get('file')));
        self.previewObject(tempModel.cid);
      },

      editObject: function editObject (cid) {
        this.dispose();

        var collection = this.selectedBucket();
        var model = collection.get(cid) || tempModel;
        viewInstance = new CMS.Views.ObjectFormView({
          model: model
        });

        $('#app').empty().html(viewInstance.render().el);
      },

      newObject: function newObject () {
        this.dispose();

        var collection = this.selectedBucket();
        tempModel = collection.newObject();
        viewInstance = new CMS.Views.ObjectFormView({
          model: tempModel
        });

        $('#app').empty().html(viewInstance.render().el);
      },

      previewObject: function previewObject (cid) {
        this.dispose();

        var collection = this.selectedBucket();
        var model = collection.get(cid) || tempModel;
        viewInstance = new CMS.Views.preview({
          model: model
        });

        $('#app').empty().html(viewInstance.render().el);
      },

      dispose: function dispose () {
        if (typeof(viewInstance.remove) === "function") {
          viewInstance.remove();
        }
        window.scroll(0, 0);
      },

      selectedBucket: function selectedBucket () {
        return _.find(CMS.Buckets, {'id': selectedBucketId});
      }
    };
  })();


})();
