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

  CMS.Forms.time = Backbone.View.extend({
    tmpl: CMS.Templates.time,
    prefix: '2015/01/01 ',

    getValue: function () {
      var value = this.$('#' + this.model.key).val();
      if (value) {
        return value;
      }
      return undefined;

    },

    render: function () {
      var value = new Date(this.prefix + this.model.value);
      if (this.model.preview || this.model.hidden) {
        value = this.model.value;
      }

      this.$el.append(this.tmpl({
        key: this.model.key,
        value: value,
        label: this.model.label,
        required: this.model.required,
        preview: this.model.preview,
        hidden: this.model.hidden
      }));

      this.$('#datetimepicker[data-key='+ this.model.key + ']').datetimepicker({
        format: 'HH:mm',
        defaultDate: (this.model.value) ? new Date(this.prefix + this.model.value) : false
      });

      return this;
    },

    validate: function () {
      var message = this.validRequire();
      if (!message) {
        message = this.validType();
      }

      return message;
    },

    validType: function () {
      var message = "";
      var error_message = this.model.label + "が正しくありません";
      var time = this.getValue();

      if (time && !time.match(/([01]\d|2[0-3]):[0-5]\d/)) {
        message = error_message;
      }

      return message;
    },

    validRequire: function () {
      var message = "";
      var error_message = this.model.label + "は入力必須項目です。";

      if (this.model.required) {
        var val = this.getValue();
        if (!val) {
          message = error_message;
        }
      }

      return message;
    }
  });
})();