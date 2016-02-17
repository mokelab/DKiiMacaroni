var handlebars = require('gulp-handlebars');
var wrap = require('gulp-wrap');
var declare = require('gulp-declare');
var concat = require('gulp-concat');
var gulp = require('gulp');
var config = require('../config').templates;

gulp.task('templates', function(){
  gulp.src(config.src)
    .pipe(handlebars())
    .pipe(wrap('Handlebars.template(<%= contents %>)'))
    .pipe(declare({
      namespace: config.namespace,
      noRedeclare: true, // Avoid duplicate declarations
    }))
    .pipe(concat(config.filename))
    .pipe(gulp.dest(config.dest));
});