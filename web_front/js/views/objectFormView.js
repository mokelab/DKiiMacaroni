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

  CMS.Views.ObjectFormView = Backbone.View.extend({
    tmpl: CMS.Templates.object_form,
    initialize: function () {
      _.bindAll(this, "copy", "delete", "errorDisplay", "getFormValue", "submit", "submitButton", "redirect", "redirectBack", "render", "validate");

      this.listenTo(this.model, 'delete:success', this.redirectBack);
    },

    copy: function (e) {
      e.preventDefault();

      var attr = this.getFormValue();
      this.model.set(attr);

      var error_message = this.validate();

      if (error_message) {
        this.errorDisplay(error_message);
        return;
      }

      CMS.Router.copyObject(this.model.cid);
    },

    delete: function (e) {
      e.preventDefault();

      if ( confirm("削除してよろしいですか？")){
        this.model.delete();
      }
    },

    errorDisplay: function (message) {
      this.$('.panel').addClass('hide');
      this.$('.panel .panel-body').html(message);
      this.$('.panel').removeClass('hide');
      window.scroll(0, 0);
    },

    events: {
      'click #back': "redirectBack",
      "click #submit": "submit",
      "click #copy": "copy",
      "click #delete": "delete"
    },

    getFormValue: function () {
      var self = this;

      var attr = {};
      _.forEach(self.model.get('fields'), function (field) {
        attr[field.key] = field.view.getValue();
      });

      return attr;
    },

    submit: function (e) {
      e.preventDefault();

      var attr = this.getFormValue();
      this.model.set(attr);

      var error_message = this.validate();

      if (error_message) {
        this.errorDisplay(error_message);
        return;
      }

      this.redirect();
    },

    submitButton: function () {
      var submit = $('<input type="submit" id="submit" class="btn btn-primary" style="margin-right: 5px;">');
      if (this.model.get('is_new')) {
        submit.val('作成');
      } else {
        submit.val('更新');
      }

      return submit;
    },

    redirect: function () {
      CMS.Router.previewObject(this.model.cid);
    },

    redirectBack: function (e) {
      if(e) {e.preventDefault();}
      CMS.Router.bucketIndexCurrentPage();
    },

    render: function () {
      var self = this;

      self.$el.empty().html(self.tmpl({id: self.model.getId()}));

      _.forEach(self.model.get('fields'), function (field) {
        var type = field.type.toLowerCase();
        field.value= self.model.get(field.key);
        field.preview = false;

        field.view = new CMS.Forms[type]({model: field});
        self.$('#form').append(field.view.render().el);
      });

      if (self.model.get('enableImageFile')) {
        var filearea = $('<div></div>');
        self.fileform = new CMS.Forms.file({model: {file: self.model.get('file')}});
        self.$('#form').append(self.fileform.render().el);
        self.model.get('file').download(self.model.get('kiiobj'))
        .then(function () {
          self.fileform.reload();
        }, function error_func (error) {
          console.error(error);
          alert("fileのダウンロードに失敗しました。やり直して下さい。");
        });
      }

      if (self.model.get('editable')) {
        self.$('#form').append(self.submitButton());
      }

      if (!self.model.get('is_new')) {
        self.$('#form').append('<input type="submit" id="copy" class="btn btn-default" value="コピーして新規作成" style="margin-right: 5px;">');
        self.$('#form').append('<button id="delete" class="btn btn-danger" >削除</button>');
      }


      return self;
    },

    validate: function () {
      var self = this;
      var message = "";

      _.forEach(self.model.get('fields'), function (field) {
        var result = field.view.validate();
        if (result) {
          if (message) { message = message + '<br>'; }
          message = message + result;
        }
      });

      var error_message = this.model.execCustomValidator();
      if (error_message) {
        if (message) { message = message + '<br>'; }
        message = message + error_message;
      }

      return message;
    }
  });
})();
