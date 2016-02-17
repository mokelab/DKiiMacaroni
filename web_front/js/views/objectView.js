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

(function () {
  'use strict';

  CMS.Views.ObjectView = Backbone.View.extend({
    tagName: 'tr',
    tmpl: CMS.Templates.object_row,

    initialize: function () {
      _.bindAll(this, 'delete', 'onClick', 'reload', 'render');

      this.listenTo(this.model, 'delete:success', this.reload);
    },

    delete: function (e) {
      e.preventDefault();

      if ( confirm("削除してよろしいですか？")){
        this.model.delete();
      }
    },

    events: {
      'click .edit': 'onClick',
      'click #delete': 'delete'
    },

    onClick: function () {
      CMS.Router.editObject(this.model.cid);
    },

    reload: function () {
      CMS.Router.bucketIndex();
    },

    render: function () {
      var self = this;

      var context = {fields: {}};

      _.forEach(self.model.get('descriptionKeys'), function (key) {
        var value;
        if (typeof(key) == 'string') {
          var field = _.findWhere(self.model.get('fields'), {key: key});
          value = self.model.get(key);
          if (field) {
            if (field.type.toLowerCase() === "geopoint") {
              value = '' + value.lat + ',' + value.lon;
            } else if (field.type.toLowerCase() === "date") {
              value = value.toLocaleString();
            }
          }
        } else if (typeof(key) == 'function') {
          value = key.call(self.model, self.model);
        }
        context.fields[key] = value;
      });

      self.$el.empty().html(self.tmpl(context));

      return self;
    }
  });
}());
