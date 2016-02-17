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

// master
// 起動方法：RESTから直接起動
// 内容：
//    ・特定期間内での読み込み回数を制限する
//    ・ObjectBodyのpublishUrlを更新する
//    ・呼ばれたログをログバケットに記録し、そこからカウントする
function master(params, context, done) {
  var BUCKET_NAME = "master";
  var GROUP_URI = "kiicloud://groups/0000000000000000000000000";
  var TIME_COLUMN_NAME = "time";
  var CONTENT_ID_COLUMN_NAME = 'content_id';
  var LOG_BUCKET_NAME = "content_access_log";
  var TO_CONDITION = new Date().getTime();

  var param = {
    app_id: context.getAppID(),
    app_key: context.getAppKey()
  };
  
  var getAccessCount = function (bucket, content_id, limit_hour, limit_count) {
    var deferred = $.Deferred();
		
    var today = new Date();
    var from_condition = today.setHours(today.getHours() - limit_hour);

    var clause1 = KiiClause.greaterThanOrEqual(TIME_COLUMN_NAME, from_condition);
    var clause2 = KiiClause.lessThanOrEqual(TIME_COLUMN_NAME, TO_CONDITION);
    var clause3 = KiiClause.equals(CONTENT_ID_COLUMN_NAME, content_id);
    var total_clause = KiiClause.and(clause1, clause2);
    total_clause = KiiClause.and(total_clause, clause3);
    var query = KiiQuery.queryWithClause(total_clause);

    if (!limit_count) {
      return deferred.resolve(-1);
    }
    bucket.countWithQuery(query, {
      success: function success (bucket, query, count) {
        deferred.resolve(count);
      },
      failure: function failure (bucket, query, error) {
        console.error(error);
        deferred.reject();
      }
    });

    return deferred.promise();
  };

  var createAccessLog = function (log_bucket, content_id) {
    var deferred = $.Deferred();

    var log = log_bucket.createObject();
    log.set(TIME_COLUMN_NAME, TO_CONDITION);
    log.set(CONTENT_ID_COLUMN_NAME, content_id);
    log.save({
      success: function success (obj) {
        deferred.resolve();
      },
      failure: function failure (obj, error) {
        console.error(error);
        deferred.reject();
      }
    });

    return deferred.promise();
  };

  var getAllValues = function (object) {
    var deferred = $.Deferred();

    var callbacks = {
      success: function (obj) {
        var content_values = Object.keys(obj._customInfo).reduce(function (result, val) {
          result[val] = obj.get(val);
          return result;
        }, {});
        deferred.resolve(content_values);
      },
      failure: function (error) {
        console.error(error);
        deferred.reject();
      }
    };

    object.refresh(callbacks);
    return deferred.promise();
  };
  
  var republishObjectBodyIfNeed = function (object, param) {
    var deferred = $.Deferred();
    
    var needRepublish = false;
    
    var publishedAt = object.get("publishedAt");
    if(publishedAt !== undefined){
      console.info("Already published.");
      var publishedDate = new Date(publishedAt);

      if (TO_CONDITION > publishedDate.getTime() + 5*60*1000){
        needRepublish = true;
      console.info("Need republish.");
      }else{
        deferred.resolve(object);
      }
    }else{
      needRepublish = true;
      console.info("Need republish.");
    }
    
    if(needRepublish){
      object.publishBody({
        success: function(obj, publishedUrl){
          console.info("Published: "+ publishedUrl);
          object.set("publishedAt", TO_CONDITION);
          object.set("publisheUrl", publishedUrl);
          object.save({
            success: function(theObject){
              console.info("Saved.");
              deferred.resolve(object);
            },
            failure: function(object){
              deferred.reject("Save is failure.");
            }
          });
        },
        failure: function(obj, anErrorString){
          console.info("Publish is failure.");
          object.set("publishedAt", undefined);
          object.set("publisheUrl", undefined);
          object.save({
            success: function(theObject){
              console.info("Saved.");
              deferred.resolve(object);
            },
            failure: function(object){
              deferred.reject("Save is failure.");
            }
          });
        }
      });
    }
    
    return deferred.promise();
  }

  var user_id = params.userID;
  if (!user_id) {done();}
  var admin = context.getAppAdminContext();
  var user = admin.userWithID(user_id);

  var content_id = params.contentID;
  var content_uri = GROUP_URI + "/buckets/" + BUCKET_NAME + "/objects/" + content_id;
  var content = admin.objectWithURI(content_uri);
  var all_values;
  var validAccessCount;

  var log_bucket = user.bucketWithName(LOG_BUCKET_NAME);

  getAllValues(content)
  .then(
    function success (content_values) {
      all_values = content_values;
      return getAccessCount(log_bucket, content_id, all_values.accessLimitHour, all_values.accessLimitCount);
    },
    function error () {
      console.error("getAllValues: error");
    }
  )
  .then(
    function success (count) {
      if (count >= all_values.accessLimitCount) {
        return $.Deferred().reject();
      }
      validAccessCount = count;
      return createAccessLog(log_bucket, content_id);
    },
    function error () {
      console.error("getAccessCount: error");
    }
  )
  .then(
    function success(){
      return republishObjectBodyIfNeed(content, param);
    },
    function error () {
      console.error("createAccessLog: error");
    }
  )
  .then(
    function success(){
      return getAllValues(content);
    },
    function error (message){
      console.error("republishObjectBodyIfNeed: error "+message);
    }
  )
  .then(
    function success(all_values){
      all_values.validAccessCount = validAccessCount;
      done(all_values);
    },
    function error(){
      console.error("getAllValues2: error");
      done();
    }
  );
}

// copyTrigger
// 起動方法：オブジェクト生成・更新のフックで起動
// 内容：
//    ・triggerバケットに特定のフィールドをコピーする
//    ・コピー後、setPublish・scheduleRegistを起動
var _ = require('underscore');
function copyTrigger (params, context, done) {
  var GROUP_URI = "kiicloud://groups/0000000000000000000000000";

  var TARGET_BUCKET_NAME = "trigger";
  var COPY_FIELDS = ["title", "triggerType", "startDateTime", "endDateTime", "startTime", "endTime", "transitionType", "enabled", "geoPoint", "range", "peripheralUUID", "major", "minor", "RSSI", "SSID", "BSSID", "target", "transitionEnter", "transitionExit", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "dayOfMonth"];
  var RELATION_FIELD_NAME = "relation";

  var objectRefresh = function objectRefresh (object) {
    var deferred = $.Deferred();

    var callbacks = {
      success: function (refreshed_object) {
        deferred.resolve(refreshed_object);
      },
      failure: function (o, e) {
        deferred.reject(e);
      }
    };

    object.refresh(callbacks);
    return deferred.promise();
  };

  var copyObject = function copyObject (obj, target) {
    var deferred = $.Deferred();

    _.each(COPY_FIELDS, function (field) {
      target.set(field, obj.get(field));
    });

    target.set(RELATION_FIELD_NAME, obj.getUUID());
    target.saveAllFields({
      success: function (obj) {
        deferred.resolve(obj);
      },
      failure: function () {
        deferred.reject();
      }
    });

    return deferred.promise();
  };

  var findTargetObject = function findTargetObject (bucket, uuid) {
    var deferred = $.Deferred();
    var clause = KiiClause.equals(RELATION_FIELD_NAME, uuid);

    var callback = {
      success: function (queryPerformed, resultSet) {
        deferred.resolve(resultSet[0]);
      },
      failure: function() {
        deferred.reject();
      }
    };

    var query = KiiQuery.queryWithClause(clause);
    bucket.executeQuery(query, callback);

    return deferred.promise();
  };

  var user = context.getAppAdminContext();
  var object_uri = params.uri;
  var object = user.objectWithURI(object_uri);
  var group = user.groupWithURI(GROUP_URI);
  var bucket = group.bucketWithName(TARGET_BUCKET_NAME);


  objectRefresh(object)
  .then(function success (refreshed_object) {
    object = refreshed_object;
    return findTargetObject(bucket, refreshed_object.getUUID());

  }, function error () {
    console.error("objectRefresh: ERROR");
  })
  .then(function success (target) {
    if (!target) {
      target = bucket.createObject();
    }

    return copyObject(object, target);
  }, function error () {
    console.error("findTargetObject: ERROR");
  })
  .then(function success (copy) {
    var deferred1 = $.Deferred();
    var deferred2 = $.Deferred();
    setPublish({uri: copy.objectURI()}, context, deferred1.resolve);
    scheduleRegist({uri: copy.objectURI()}, context, deferred2.resolve);
    $.when(deferred1.promise(), deferred2.promise()).done(function () {done()});
  }, function error () {
    console.error("copyObject: ERROR");
    done();
  });
}


function registerLog (params, context, done) {
  var LOG_BUCKET_NAME = "log";
  var TRIGGER_ID_FIELD_NAME = "trigger_id";
  var NEWS_ID_FIELD_NAME = "news_id";
  var USER_ID_FIELD_NAME = "user_id";
  var TIME_FIELD_NAME = "time";
  var HOUR_FIELD_NAME = "hour";

  var admin = context.getAppAdminContext();
  var trigger_id = params.trigger_id;
  var news_id = params.news_id;
  var user_id = params.userID;
//  var time = Math.floor(new Date().getTime() / 1000);
  var time = Number(params.unixtime);
  var hour = Number(params.hour);

  var log_bucket = admin.bucketWithName(LOG_BUCKET_NAME);
  var log_object = log_bucket.createObject();
  if (trigger_id) {
    log_object.set(TRIGGER_ID_FIELD_NAME, trigger_id);
  }
  if (news_id) {
    log_object.set(NEWS_ID_FIELD_NAME, news_id);
  }
  log_object.set(USER_ID_FIELD_NAME, user_id);
  log_object.set(TIME_FIELD_NAME, time);
  log_object.set(HOUR_FIELD_NAME, hour);

  log_object.save({
    success : function () {
      done({});
    },
    failure: function(theObject, errorString) {
      done(errorString);
    }
  });
}


function getLogCount (params, context, done) {
  var LOG_BUCKET_NAME = "log";
  var TRIGGER_ID_FIELD_NAME = "trigger_id";
  var NEWS_ID_FIELD_NAME = "news_id";
  var USER_ID_FIELD_NAME = "user_id";
  var TIME_FIELD_NAME = "time";
  var HOUR_FIELD_NAME = "hour";

  var admin = context.getAppAdminContext();
  var trigger_id = params.trigger_id;
  var news_id = params.news_id;
  var start_time = Number(params.start_time);
  var end_time = Number(params.end_time);
  var hour = Number(params.hour);

  var start_time_clause = KiiClause.greaterThanOrEqual(TIME_FIELD_NAME, start_time);
  var end_time_clause = KiiClause.lessThanOrEqual(TIME_FIELD_NAME, end_time);
  var extra_clause;
  if (trigger_id) {
    extra_clause = KiiClause.equals(TRIGGER_ID_FIELD_NAME, trigger_id);
  }
  else if (news_id) {
    extra_clause = KiiClause.equals(NEWS_ID_FIELD_NAME, news_id);
  }
  var all_query = KiiClause.and(start_time_clause, end_time_clause, extra_clause);
  if (hour) {
    all_query = KiiClause.and(all_query, KiiClause.equals(HOUR_FIELD_NAME, hour));
  }
  var query = KiiQuery.queryWithClause(all_query);

  var log_bucket = admin.bucketWithName(LOG_BUCKET_NAME);
  log_bucket.countWithQuery(
    query,
    {
      success: function (bucket, query, count) {
        done({count:count});
      },
      failure: function (bucket, errorString) {
        done(errorString);
      }
    }
  );
}


// scheduleRegist
// 起動方法：オブジェクト生成・更新のフックで起動、copyTriggerから直接起動
// 内容：
//    ・topicのpushスケジュールを登録
//    ・スケジュールのタスクIDをオブジェクトに更新
function scheduleRegist (params, context, done) {
  var TASK_ID_COLUMN_NAME = "task_id";
  var PUBLISH_DATE_COLUMN_NAME = "startDateTime";
  var TITLE_COLUMN_NAME = "title";
  var MESSAGE_COLUMN_NAME = "message";
  var TOPIC_COLUMN_NAME = "topic";

  var objectRefresh = function object_refresh (object) {
    var deferred = $.Deferred();

    var callbacks = {
      success: function (refreshed_object) {
        deferred.resolve(refreshed_object);
      },
      failure: function (o, e) {
        deferred.reject(e);
      }
    };

    object.refresh(callbacks);
    return deferred.promise();
  };

  var createBody = function createBody (object, param) {
    var publishDate = object.get(PUBLISH_DATE_COLUMN_NAME);
    var title = object.get(TITLE_COLUMN_NAME);
    var msg = object.get(MESSAGE_COLUMN_NAME);
    var topic = object.get(TOPIC_COLUMN_NAME);
    
    var date_type_check = new Date(publishDate);
    if (!publishDate) {console.error("ERROR"); return "ERROR";}
    if (!date_type_check.valueOf()) {console.error("ERROR"); return "ERROR";}
    if (date_type_check < new Date()) {console.error("ERROR"); return "ERROR";}

    return {
      description: 'publish',
      what: "REST_API_CALL",
      when: publishDate,
      params: {
        uri: "/api/apps/" + param.app_id + "/topics/" + topic + "/push/messages",
        method: "POST",
        headers: {
          "Content-Type": "application/vnd.kii.SendPushMessageRequest+json"
        },
        body: {
          data: {
            MsgBody: msg,
            Priority: 1,
            Urgent: false
          },
          sendToDevelopment: true,
          gcm: {
            enabled: true
          },
          apns: {
            enabled: true,
            alert: { body: title }
          }
        }
      }
    };
  };

  var taskCancel = function taskCancel (task_id, param) {
    $.ajax({
      type: 'PUT',
      url: param.url + "/" + task_id + "/status/CANCELLED",
      headers: {
        'Authorization': "Bearer " + param.access_token,
        'X-Kii-AppID': param.app_id,
        'X-Kii-AppKey': param.app_key
      },
      success: function success (data, status, xhr) {
        console.log("CANCEL_STATUS:", status);
      },
      error: function error (xhr, status, err) {
        console.error("CANCEL_STATUS:", status);
      }
    });
  };

  var taskRegist = function taskRegist (param) {
    var deferred = $.Deferred();

    $.ajax({
      type: 'POST',
      url: param.url,
      contentType: 'application/vnd.kii.TaskCreationRequest+json',
      headers: {
        'Authorization': "Bearer " + param.access_token,
        'X-Kii-AppID': param.app_id,
        'X-Kii-AppKey': param.app_key
      },
      data: JSON.stringify(param.body),

      success: function success (data, status, xhr) {
        deferred.resolve(data.taskID);
      },

      error: function error (request, status, err) {
        console.error(request.responseText);
        deferred.reject(status);
      }
    });

    return deferred.promise();
  };

  var user = context.getAppAdminContext();
  var object_uri = params.uri;
  var object = user.objectWithURI(object_uri);
  var access_token = user._getToken();

  var param = {
    app_id: context.getAppID(),
    app_key: context.getAppKey(),
    access_token: access_token
  };

  Kii.initializeWithSite(param.app_id, param.app_key, KiiSite.JP);
  param.url = Kii.getBaseURL() + "/apps/" + param.app_id + "/tasks";

  objectRefresh(object)
  .then(
    function success (refreshed_object) {
      param.body = createBody(refreshed_object, param);
      if (param.body === "ERROR") {
        console.log("createBody: ERROR");
        return $.Deferred().reject();
      }

      var task_id = refreshed_object.get(TASK_ID_COLUMN_NAME);
      if (task_id) {
        taskCancel(task_id, param);
      }

      // task登録
      return taskRegist(param);
    },
    function error (e) {
      console.error("objectRefresh: ERROR");
    }
  )
  .then(
    function success (task_id) {
      // taskidをobjectに登録
      object.set(TASK_ID_COLUMN_NAME, task_id);

      object.save({
        success: function success () { done(); },
        failure: function failure () { console.error("task_id set: ERROR"); done(); }
      });
    },
    function error () {
      console.error("taskRegist: ERROR");
      done();
    }
  );
}


// updateACL
// 起動方法：スケジュール起動で起動、setPublish内から直接起動
// 内容：
//    (Enable == true) && (publishStart <= now) && (now < publishEnd)が
//     真ならACL付与、偽ならACL剥奪
function updateACL (params, context, done) {
//  var START_DATE_COLUMN_NAME = "publishStart";
//  var END_DATE_COLUMN_NAME = "publishEnd";
  var START_DATE_COLUMN_NAME = "startDateTime";
  var END_DATE_COLUMN_NAME = "endDateTime";
  var ENABLE_COLUMN_NAME = "enabled";

  var getGrant = function getGrant () {
    return KiiACLEntry.entryWithSubject(new KiiAnyAuthenticatedUser(), KiiACLAction.KiiACLObjectActionRead);
  };

  var setACL = function setACL (object, entry) {
    var deferred = $.Deferred();
    var acl = object.objectACL();
    acl.putACLEntry(entry);
    acl.save({
      success: function () {
        deferred.resolve();
      },
      failure: function () {
        deferred.resolve();
      }
    });
    return deferred.promise();
  };

  var objectRefresh = function object_refresh (object) {
    var deferred = $.Deferred();

    var callbacks = {
      success: function (refreshed_object) {
        deferred.resolve(refreshed_object);
      },
      failure: function (o, e) {
        deferred.reject(e);
      }
    };

    object.refresh(callbacks);
    return deferred.promise();
  };

  var getStatus = function status_set (obj) {
    var now = new Date().getTime();
    var publish_start = obj.get(START_DATE_COLUMN_NAME);
    var publish_end = obj.get(END_DATE_COLUMN_NAME);

    return {
      enable: obj.get(ENABLE_COLUMN_NAME),
      is_started: publish_start <= now,
      is_ended: publish_end <= now,
      publish_start: publish_start,
      publish_end: publish_end
    };
  };

  var admin = context.getAppAdminContext();
  var content = admin.objectWithURI(params.object_uri);
  var status = {};

  objectRefresh(content)
  .then(function success (refreshed_content) {
    var entry = getGrant();
    status = getStatus(refreshed_content);

    if (!(status.enable && status.is_started && !status.is_ended)) {
      entry.setGrant(false);
    }

    return setACL(refreshed_content, entry);
  }, function error () {
    console.error("objectRefresh: ERROR");
  })
  .then(function success () {
    done(status);
  }, function error () {
    console.error("setACL: ERROR");
    done(false);
  });
}

// setPublish
// 起動方法：オブジェクト生成・更新のフックで起動、copyTriggerから直接起動
// 内容：
//   ・updateACLを実行し、現在のACLを更新
//   ・まだ公開状態にない場合は公開のタスクスケジュール(updateACL)を登録
//   ・まだ公開終了してない場合は公開終了のタスクスケジュール(updateACL)を登録
function setPublish (params, context, done) {
  var execUpdateAcl = function execUpdateAcl (params, context) {
    var deferred = $.Deferred();
    updateACL(params, context, deferred.resolve);
    return deferred.promise();
  };

  var createBody = function createBody (time, param) {
    return {
      description: 'publish',
      what: "REST_API_CALL",
      when: time,
      params: {
        uri: "/api/apps/" + param.app_id + "/server-code/versions/current/updateACL",
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: {
          object_uri: param.object_uri
        }
      }
    };
  };

  var taskRegist = function taskRegist (param) {
    var deferred = $.Deferred();

    $.ajax({
      type: 'POST',
      url: param.url,
      contentType: 'application/vnd.kii.TaskCreationRequest+json',
      headers: {
        'Authorization': "Bearer " + param.access_token,
        'X-Kii-AppID': param.app_id,
        'X-Kii-AppKey': param.app_key
      },
      data: JSON.stringify(param.body),

      success: function success (data, status, xhr) {
        deferred.resolve(data.taskID);
      },

      error: function error (request, status, err) {
        console.error(request.responseText);
        deferred.reject(status);
      }
    });

    return deferred.promise();
  };

  var param = {
    object_uri: params.uri,
    app_id: context.getAppID(),
    app_key: context.getAppKey(),
    access_token: context.getAppAdminContext()._getToken()
  };

  Kii.initializeWithSite(param.app_id, param.app_key, KiiSite.JP);
  param.url = Kii.getBaseURL() + "/apps/" + param.app_id + "/tasks";

  execUpdateAcl(param, context)
  .then(function (status) {
    console.log(status)
    var cont = $.Deferred().resolve();
    if (status) {
      if (status.enable && !status.is_started) {
        param.body = createBody(status.publish_start, param);
        cont = taskRegist(param);
      }
      if (status.enable && !status.is_ended) {
        param.body = createBody(status.publish_end, param);
        cont = taskRegist(param);
      }
    }

    return cont;
  })
  .then(function success () {
    done();
  }, function error () {
    done();
  });
}
