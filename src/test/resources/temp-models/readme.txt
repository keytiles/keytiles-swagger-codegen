
During running tests these resource folder will have a "copy" under /target folder 
And generators from test code might use that folder to output their generated result

This should be considered as a temp folder, cleaned up automatically at the end of the test (ideally)

But even if it is not done the mechanism is safe because - as we wrote - the real folder will be under /target somewhere
Thus never goes anything back to Git and always fully cleaned with 'mvn clean'

So we are good :-)