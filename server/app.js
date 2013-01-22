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

var express = require('express');
var server = express();
server.use(express.bodyParser());


var PersonSchema = new Schema({
  first_name: String,
  last_name: String,
  is_awesome: Boolean
});
Person = mongoose.model('Person', PersonSchema);

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
server.post('/people', postPerson);
var port = process.env.PORT || 8080;
server.listen(port, function(){
  console.log('listening at %s', port);
});
