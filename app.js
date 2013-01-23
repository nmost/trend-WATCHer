//mongoose includes
var mongoose = require("mongoose")
var uri =   process.env.MONGODB_URI || 
  process.env.MONGOLAB_URI || 
    'mongodb://localhost/HelloMongoose';
db = mongoose.connect(uri,  function (err, res) {
    if (err) { 
      console.log ('ERROR connecting to: ' + uri + '. ' + err);
    } else {
      console.log ('Succeeded connected to: ' + uri);
    }
});
Schema = mongoose.Schema;

//Express stuff
var express = require('express');
var server = express();
server.use(express.bodyParser());

//GCM stuff
var gcm = require('node-gcm');

//DATABASE INIT
var ObjectId = mongoose.Schema.Types.ObjectId;
var UserSchema = new Schema({
  registration_id: String,
  trend: { type: ObjectId, ref: 'Trend'},
  is_watching: { type: Boolean, default: false}
});
var TrendSchema = new Schema({
  trend: String,
  tweets: { type: ObjectId, ref: 'Tweet'},
  users: {type: ObjectId, ref: 'User'},
  newcounter: Number
});
var TweetSchema = new Schema({
  tweeter: String,
  time: { type: Date, default: Date.now },
  content: String,
  trend: { type: ObjectId, ref: 'Trend'},
});

User = mongoose.model('User', UserSchema);
Trend = mongoose.model('Trend', TrendSchema);
Tweet = mongoose.model('Tweet', TweetSchema);


//////
//Initialize a user
////
function initializeUser(req, res, next){
  var user = new User();
  user.registration_id = req.body.regId;
  user.save(function(){
    res.send(user);
    pingUser(user.registration_id);
  });
}


//////
//Set the currently trending topic
////
function setTrending(req, res, next){


}

//////
//flip the user's is_watching
////
function flipStatus(req, res, next){
  User.findById(req.body._id, function(err, user){
    user.is_watching = (!user.is_watching);
  });
}

//////
//Post the tweets to a user
////
function pingUser(ids){
  var message = new gcm.Message();
  var sender = new gcm.Sender('AIzaSyDx1b8eGfFYEmAgrwp7qgTwU3SSU9_1mu4');
  
  sender.send(message, ids, 5, function(err, result){
    console.log(result);
  });
}
/*
function getPeople(req, res, next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "X-Requested-With");
  Person.find().limit(100).execFind(function(arr,data){
    res.send(data);
  });
}

function postPerson(req,res,next) {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "X-Requested-With");
  var person = new Person();
  person.first_name = req.body.first_name;
  person.last_name = req.body.last_name;
  person.is_awesome = req.body.is_awesome;
  person.save(function(){
    Person.find().limit(100).execFind(function(arr,data){
     res.send(data);
    });
  });
}

server.get('/people', getPeople);
server.post('/people', postPerson);*/
server.post('/newuser', initializeUser);
var port = process.env.PORT || 8080;
server.listen(port, function(){
  console.log('listening at %s', port);
});
