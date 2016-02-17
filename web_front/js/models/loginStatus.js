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

  CMS.Models.LoginStatus = Backbone.Model.extend({
    defaults: {
      loggedIn: false,
      name: null,
      password: null
    },

    initialize: function () {
      this.on('change', this.login, this);
      this.on('change:loggedIn', this.onStatusChange, this);
    },

    isMember: function () {
      var deferred = $.Deferred();
      var kiiuser = Kii.getCurrentUser();

      kiiuser.memberOfGroups({
        success: function (theUser, groupList) {
          var uri_list = _.invoke(groupList, 'objectURI');
          var is_member = _.find(uri_list, function (uri) { return uri === CMS.Config.groupURI; });

          var result = (is_member) ? deferred.resolve() : deferred.reject();
        }
      });

      return deferred.promise();
    },

    login: function () {
      var self = this;
      if (this.get('loggedIn')) { return; }

      if (!(self.get('name'))) { self.loginError(); return; }
      if (!(self.get('password'))) { self.loginError(); return; }

      KiiUser.authenticate(self.get('name'), self.get('password'), {
        success: function () {
          self.isMember().then(function () {
            self.set({'loggedIn': true});
          }, function () {
            self.loginError();
          });
        },
        failure: function (o, e) {
          console.log(e);
          self.loginError();
        }
      });
    },

    loginError: function () {
      this.trigger('login:error');
    },

    onStatusChange: function () {
      if (this.get('loggedIn')) {
        this.trigger('login:success');
      }
    },

    setAuth: function (name, password) {
      this.set({'name': name, 'password': password});
    }
  });
})();
