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

  CMS.Forms.geopoint = Backbone.View.extend({
    tmpl: CMS.Templates.geopoint,

    events: {
      "change input.lat": "displayMaps",
      "change input.lon": "displayMaps",
    },

    displayMaps: function () {
      var url = "http://www.mapquestapi.com/staticmap/v4/getmap?key={{key}}&size=200,200&zoom=14&center={{lat}},{{lon}}&pois=mcenter,{{lat}},{{lon}}";

      var geo = this.getValue();
      if (this.model.preview) { geo = this.model.value; }
      if (!geo) { geo = {lat: '', lon: ''}; }

      url = url
        .replace(/{{key}}/g, CMS.Config.mapQuestKey)
        .replace(/{{lat}}/g, geo.lat)
        .replace(/{{lon}}/g, geo.lon);

      this.$('.map').attr("src", url);
    },

    getValue: function () {
      var lat_str = this.$('#' + this.model.key + '.lat').val();
      var lon_str = this.$('#' + this.model.key + '.lon').val();

      var lat = (lat_str) ? Number(lat_str) : "";
      if (isNaN(lat)) { lat = lat_str; }
      var lon = (lon_str) ? Number(lon_str) : "";
      if (isNaN(lon)) { lon = lon_str; }

      if (!lat && !lon) {
        return undefined;
      }

      return {lat: lat, lon: lon};
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
      this.displayMaps();

      return this;
    },

    validate: function () {
      var message = this.validType();
      if (!message) {
        message = this.validRequire();
      }
      if (!message) {
        message = this.validRange();
      }

      return message;
    },

    validType: function () {
      var message = "";
      var error_message = this.model.label + "が正しくありません";
      var value = this.getValue();

      if (value && _.every(value, function (v) {return v !== "";})) {
        if (!(typeof value.lat === "number" && typeof value.lon === "number")) {
          message = error_message;
        }
      }

      return message;
    },

    validRange: function () {
      var message = "";
      var lat_error_message = this.model.label + ":Latitudeは-90〜90の範囲で入力してください。";
      var lon_error_message = this.model.label + ":Longitudeは-180〜180の範囲で入力してください。";
      var value = this.getValue();

      if (value && (value.lat >= 90 || value.lat <= -90)) {
        message = lat_error_message;
      }

      if (value && (value.lon >= 180 || value.lon <= -180)) {
        message = lon_error_message;
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