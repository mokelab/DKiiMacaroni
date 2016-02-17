var dest = '.';
var src = '.';
var path = require('path');
var relativeSrcPath = path.relative('.', src);

module.exports = {
  dest: dest,
  
  js: {
    src: src + '/js/**',
    dest: dest + '/js',
    uglify: false
  },

  webpack: {
    entry: src + '/js/app.js',
    output: {
      filename: 'bundle.js'
    },
    resolve: {
      extensions: ['', '.js']
    }
  },

  watch: {
    js: relativeSrcPath + '/js/**',
    handlebars: src + '/templates/**'
  },

  templates: {
    src: src + '/templates/*.hbs',
    dest: src + '/js/templates/',
    namespace: 'CMS.Templates',
    filename: 'layout.js'
  }
};
