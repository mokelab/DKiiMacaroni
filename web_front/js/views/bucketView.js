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

  CMS.Views.BucketView = Backbone.View.extend({
    tagName: 'div',
    tmpl: CMS.Templates.bucket,

    events: {
      'click #back': 'redirectBack',
      'click #new': 'redirectNew',
      'click #to-top': 'goToFirst',
      'click #to-next': 'goToNext'
    },

    initialize: function() {
      this.listenTo(this.collection, 'turn', this.render, this);
    },

    goToFirst: function (e) {
      e.preventDefault();
      CMS.Router.bucketIndex();
    },

    goToNext: function (e) {
      e.preventDefault();
      CMS.Router.bucketIndexNextPage();
    },

    redirectBack: function (e) {
      e.preventDefault();
      CMS.Router.buckets();
    },

    redirectNew: function (e) {
      e.preventDefault();
      CMS.Router.newObject();
    },

    render: function () {
      var self = this;

      var labels = _.reduce(self.collection.descriptionKeys, function (result, key) {
        var field = _.findWhere(self.collection.fields, {key: key});
        var label = (field && field.label) ? field.label : key;
        return result.concat(label);
      }, []);

      var context = {
        fields: labels,
        has_next: self.collection.nextQuery,
        is_head: self.collection.is_head
      };
      self.$el.empty().html(self.tmpl(context));

      self.collection.each(function (model) {
        var view = new CMS.Views.ObjectView ({
          model: model
        });
        self.$('#objects').append(view.render().el);
      });

      return self;
    }
  });
})();