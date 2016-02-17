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

  CMS.Models.ObjectBase = Backbone.Model.extend({

    initialize: function () {
      _.bindAll(this, 'save', "delete", "execFilter", 'execCustomValidator', 'getId', 'toObject', 'toGeoPoint', 'toUnixTime', 'updateKiiobj');
      var uuid = (_.findWhere(this.get('fields'), {key: 'uuid'})) ? this.get('uuid') : this.getId();
      this.set('uuid', uuid);
      this.set('file', new CMS.Util.FileLoader());
    },

    delete: function () {
      var self = this;
      var kiiobj = this.get('kiiobj');
      if (_.contains(_.pluck(this.get('fields'), 'key'), CMS.Config.enabledFieldName)) {
        this.set(CMS.Config.enabledFieldName, false);
        kiiobj.set(CMS.Config.enabledFieldName, false);
      }

      this.set(CMS.Config.deletedFieldName, true);
      kiiobj.set(CMS.Config.deletedFieldName, true);

      kiiobj.save({
        success: function () {
          self.trigger('delete:success');
        },
        failure: function (o, e) {
          alert(e);
        }
      });
    },

    execFilter: function () {
      var fields = this.toObject();
      var clone = $.extend(true, {}, fields);

      _.forEach(this.get('fields'), function (field) {
        if (typeof field.filter === 'function') {
          fields[field.key] = field.filter(fields[field.key], $.extend(true, {}, clone));
        }
      });

      return fields;
    },

    execCustomValidator: function () {
      var self = this;
      var fields = this.toObject();
      var clone = $.extend(true, {}, fields);

      var error_message = "";
      _.forEach(self.get('fields'), function (field) {
        if (typeof field.validator === 'function') {
          var result = field.validator(_.clone(self.get(field.key)), $.extend(true, {}, clone));
          if (result) {
            if (error_message) { error_message = error_message + '<br>'; }
            error_message = error_message + result;
          }
        }
      });

      return error_message;
    },

    getId: function () {
      return this.get('kiiobj').getUUID();
    },

    save: function () {
      var self = this;
      if (!self.get('editable')) return false;

      self.updateKiiobj().saveAllFields({success: function (saved_kiiobj) {
        if (self.get('file').url()) {
          self.get('file').upload(saved_kiiobj)
          .then(function success () {
            self.trigger('save:success');
          });
        } else {
          self.trigger('save:success');
        }

      },
      failure: function (o, e) {
        alert(e);
      }});
    },

    toObject: function () {
      var self = this;

      return _.reduce(self.get('fields'), function (result, field) {
        var type = field.type.toLowerCase();
        var key = field.key;

        if (_.isUndefined(self.get(key))) {
          result[key] = self.get(key);
        } else if (type === "geopoint") {
          result[key] = self.toGeoPoint(self.get(key));
        } else if (type === "date") {
          result[key] = self.toUnixTime(self.get(key));
        } else {
          result[key] = self.get(key);
        }

        return result;
      }, {});
    },

    toGeoPoint: function (geo) {
      var kiigeo;
      try {
        kiigeo = KiiGeoPoint.geoPoint(Number(geo.lat), Number(geo.lon));
      } catch(e) {
        kiigeo = null;
      }
      return kiigeo;
    },

    toUnixTime: function (dateString) {
      var date = new Date (dateString);
      return date.getTime();
    },

    updateKiiobj: function () {
      var kiiobj = this.get('kiiobj');
      var fields = this.execFilter();

      // fieldの値をkiiObjectに設定
      _.forEach(fields, function (value, key) {
        if (value instanceof KiiGeoPoint) {
          kiiobj.setGeoPoint(key, value);
        } else {
          kiiobj.set(key, value);
        }
      });
      kiiobj.set(CMS.Config.deletedFieldName, false);

      return kiiobj;
    }

  });
})();
