#!/usr/bin/env python

import requests
response = requests.get("http://10.0.10.159:8080/orders/oom")
print("Received " + str(response.status_code))
