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

  CMS.Views.BucketsView = Backbone.View.extend({
    tagName: 'div',
    className: 'list-group',
    tmpl: CMS.Templates.buckets,

    events: {
      'click a': 'onClick'
    },

    onClick: function (e) {
      e.preventDefault();

      var id = $(e.currentTarget).data("id");
      CMS.Router.bucketIndex(id);
    },

    render: function () {
      var self = this;

      var context = {
        collection: self.collection.filter(function (item) {
          return item.visible
        })
      };

      self.$el.empty().html(self.tmpl(context));

      return self;
    }
  });
})();