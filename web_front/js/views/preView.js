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

  CMS.Views.preview = Backbone.View.extend({
    tmpl: CMS.Templates.preview,
    initialize: function () {
      this.listenTo(this.model, 'save:success', this.redirect);
    },

    events: {
      'click #back': "redirectBack",
      "click #submit": "submit"
    },

    submit: function () {
      this.model.save();
    },

    redirect: function () {
      CMS.Router.bucketIndex();
    },

    redirectBack: function () {
      CMS.Router.editObject(this.model.cid);
    },

    render: function () {
      var self = this;

      self.$el.empty().html(self.tmpl({id: self.model.getId()}));

      var fields = [];
      _.forEach(self.model.get('fields'), function (field) {
        var type = field.type.toLowerCase();
        field.value= self.model.get(field.key);
        field.preview = true;

        field.view = new CMS.Forms[type]({model: field});
        self.$('#form').append(field.view.render({preview: true}).el);
      });

      if (self.model.get('file').url()) {
        var filearea = $('<div></div>');
        self.$('#form').append(filearea);
        self.fileform = new CMS.Forms.file({model: {
          file: this.model.get('file'),
          preview: true
        }});
        filearea.append(self.fileform.render().el);
      }

      self.$('#form').append('<input type="submit" id="submit" class="btn btn-success" value="保存">');

      return self;
    }
  });
})();
