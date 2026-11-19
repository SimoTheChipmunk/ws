package com.example.weatherstation.domain

class LocationNotFoundException(id: Long) : RuntimeException("Location $id does not exist")
class DuplicateLocationException(name: String) : RuntimeException("A location named '$name' already exists")
class InvalidRequestException(message: String) : RuntimeException(message)
