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
  
  CMS.Views.Login = Backbone.View.extend({
    tmpl: CMS.Templates.login,
    
    initialize: function () {
      this.listenTo(this.model, 'login:success', this.onLogin);
      this.listenTo(this.model, 'login:error', this.onLoginError);
    },
    
    events: {
      'submit': 'onSubmit'
    },
    
    onSubmit: function (e) {
      e.preventDefault();
      var name = this.$('input[name=name]').val();
      var password = this.$('input[name=password]').val();
      this.model.setAuth(name, password);
    },
    
    onLogin: function () {
      if (CMS.Config.scriptsBucketName) {
        CMS.Util.Download.exec().then(function () { CMS.Router.buckets(); });
      } else {
        CMS.Router.buckets();
      }
    },
    
    onLoginError: function () {
      this.$('.panel').addClass('hide');
      this.$('.panel .panel-body').html("ログインに失敗しました");
      this.$('.panel').removeClass('hide');
    },
    
    render: function () {
      var self = this;
      $(self.el).empty().html(self.tmpl(self.model));
      return self;
    }
  });
})();