var app = angular.module('app', []);
app.controller('postcontroller', function($scope, $http, $location) {
	$scope.submitForm = function(){
		var url = "http://localhost:8080/api/swiftpost/send";
		var config = {headers : {'Accept': 'text/plain'}}
		var data = $scope.firstname;
		$http.post(url, data, config).then(function (response) {
			$scope.postResultMessage = response.data;
		}, function error(response) {
			$scope.postResultMessage = "Error with status: " +  response.statusText;
		});

		$scope.firstname = "";
		$scope.lastname = "";
	}
});