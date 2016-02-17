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

  CMS.Forms.string = Backbone.View.extend({
    tmpl: CMS.Templates.string,

    getValue: function () {
      var val = this.$('#' + this.model.key).val();
      if (val === "") {
        return undefined;
      }
      return val;
    },

    render: function () {
      this.$el.append(this.tmpl({
        key: this.model.key,
        value: this.model.value,
        label: this.model.label,
        required: this.model.required,
        preview: this.model.preview,
        hidden: this.model.hidden
      }));

      return this;
    },

    validate: function () {
      var message = this.validType();
      if (!message) {
        message = this.validRequire();
      }

      return message;
    },

    validType: function () {
      var message = "";
      var error_message = this.model.label + "が正しくありません";
      var value = this.getValue();

      if (value && typeof value !== "string") {
        message = error_message;
      }

      return message;
    },

    validRequire: function () {
      var message = "";
      var error_message = this.model.label + "は入力必須項目です。";

      if (this.model.required) {
        var val = this.getValue();
        if (_.isUndefined(val)) {
          message = error_message;
        }
      }

      return message;
    }
  });
})();