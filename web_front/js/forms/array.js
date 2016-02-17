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

  CMS.Forms.array = Backbone.View.extend({
    tmpl: CMS.Templates.array,
    initialize: function () {
      var self = this;

      // 要素追加時のデフォルト値配列
      self.defaults = _.map(self.model.fields, function (field) {
        var field_set =  $.extend(true, {}, field);
        var type = field.type.toLowerCase();
        if (type === "date" && field.defaultValue ) {
          field_set.value = new Date(field.defaultValue);
        } else if (!(type === "geopoint" || type === "reference" || type === "array")) {
          field_set.value = _.clone(field.defaultValue);
        } else {
          field_set.value = "";
        }

        return field_set;
      });

      if (_.isArray(self.model.value)) {
        // fieldの情報とvalueをmerge
        self.collection = _.map(self.model.value, function (data) {
          return _.map(self.model.fields, function (field) {
            var field_set = $.extend(true, {}, field);
            field_set.value = data[field.key];

            return field_set;
          });
        });
      } else { // 初期値が空の場合はデフォルトのオブジェクトを作る
        self.collection = [$.extend(true, {}, this.defaults)];
      }
    },

    events: {
      "click #add": "add"
    },

    add: function (e) {
      e.preventDefault();
      this.collection.push($.extend(true, {}, this.defaults));

      this.render();
    },

    getValue: function () {
      return _.map(this.collection, function (record) {
        var field_set = {};

        _.forEach(record, function (field) {
          if (field.type.toLowerCase() === "date") {
            var value = field.view.getValue();
            field_set[field.key] = (value) ? value.getTime() : value;
          } else {
            field_set[field.key] = field.view.getValue();
          }
        });

        return field_set;
      });
    },

    render: function () {
      var self = this;

      self.$el.empty().html(self.tmpl({
        label: self.model.label,
        required: self.model.required,
        preview: self.model.preview
      }));

      _.forEach(this.collection, function (record, index) {
        var row = $('<div class="row" id="row' + index + '"></div>');

        _.forEach(record, function (field) {
          var type = field.type.toLowerCase();
          var containar = $('<div class="col-sm-3"></div>');

          if (field.view) {
            field.value = field.view.getValue();
          }
          field.preview = self.model.preview;
          field.view = new CMS.Forms[type]({model: field});
          containar.append(field.view.render().el);

          row.append(containar);
        });

        self.$('#content').append(row);
      });

      return self;
    },

    validate: function () {
      var self = this;
      var message = "";

      _.forEach(self.collection, function (record) {
        _.forEach(record, function (field) {
          if (!message) {
            message = field.view.validate();
          }
        });
      });

      if (!message) {
        message = this.validType();
      }
      if (!message) {
        message = this.validRequire();
      }

      return message;
    },

    validType: function () {
      var message = "";
      var error_message = this.model.label + "が正しくありません";
      var value = this.getValue();

      if (!_.isArray(value)) {
        message = error_message;
      }

      return message;
    },

    validRequire: function () {
      var message = "";
      var error_message = this.model.label + "は入力必須項目です。";

      if (this.model.required) {
        var val = this.getValue();
        if (val.length === 0) {
          message = error_message;
        }
      }

      return message;
    }
  });
})();